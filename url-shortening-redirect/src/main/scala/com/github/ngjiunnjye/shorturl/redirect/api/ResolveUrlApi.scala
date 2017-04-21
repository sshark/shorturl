package com.github.ngjiunnjye.shorturl.redirect.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.apply
import akka.http.scaladsl.server.Directive.{addByNameNullaryApply, addDirectiveApply}
import akka.http.scaladsl.server.Directives.{Segment, complete, get, path, redirect, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.ngjiunnjye.cryptor.Base62
import com.github.ngjiunnjye.shorturl.redirect.actor.QueryStatus
import com.github.ngjiunnjye.shorturl.utils.Config
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class UrlShorteningRequest(sourceUrl: String, targetUrl: Option[String], requestTime : Option[Long])

object JsProtocol extends DefaultJsonProtocol with Config {
  implicit val urlFormat = jsonFormat3(UrlShorteningRequest)
}

trait ResolveUrlApi extends DefaultJsonProtocol {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  import JsProtocol._

  val urlResolverActor: ActorRef

  val urlRoute = path("unabletofindsitetoredirectto") {
      get {
        complete("Unable to find site to redirect to")
      }
    } ~ path(Segment) { shortUrl => {
      println(s"Request received $shortUrl")

      implicit val timeout = Timeout(30 seconds)

      val f = Future.fromTry(Base62.decode(shortUrl)).zip(urlResolverActor ? shortUrl)
      onComplete(f){x =>
          if (x.get._1 == nodeId) {
            val queryStatus = x.asInstanceOf[QueryStatus]
            if (queryStatus.status) redirect(queryStatus.message, StatusCodes.MovedPermanently)
            else redirect("unabletofindsitetoredirectto", StatusCodes.PermanentRedirect)
          } else redirect(s"http://${readerNodeAddresses.get(x.get._1.toInt)}/${shortUrl}", StatusCodes.MovedPermanently)
      }
    }
  }
}

/*
    val respNode = ( % 2).toInt
    if (respNode == nodeId) {
      implicit val timeout = Timeout(30 seconds)
      val future = urlResolverActor ? shortUrl
      val result = Await.result(future, timeout.duration).asInstanceOf[QueryStatus]
      if (result.status) redirect(result.message, StatusCodes.MovedPermanently)
      else {
        println(result.message)
        redirect("unabletofindsitetoredirectto", StatusCodes.PermanentRedirect)
      }
    } else {
      println(s"Redirect to node ${respNode}")
      redirect(s"http://${readerNodeAddresses.get(respNode)}/${shortUrl}", StatusCodes.MovedPermanently)
    }
  }
*/

