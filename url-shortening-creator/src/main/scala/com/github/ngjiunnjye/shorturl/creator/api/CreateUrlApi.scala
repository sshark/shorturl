package com.github.ngjiunnjye.shorturl.creator.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.sprayJsonUnmarshaller
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directive.{addByNameNullaryApply, addDirectiveApply}
import akka.http.scaladsl.server.Directives.{as, complete, decodeRequest, entity, get, path, post, _}
import akka.http.scaladsl.server.StandardRoute
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.ngjiunnjye.cryptor.Base62
import com.github.ngjiunnjye.kafka.{Producer => KafkaProducer}
import com.github.ngjiunnjye.shorturl.creator.actor.InsertStatus
import com.github.ngjiunnjye.shorturl.utils._
import org.apache.kafka.clients.producer.ProducerRecord
import spray.json.{DefaultJsonProtocol, pimpAny}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


trait UrlApi extends DefaultJsonProtocol with Config {
  val kafkaProducer = KafkaProducer.createStringStringProducer
   
  implicit val system: ActorSystem 
  implicit val materializer: ActorMaterializer 
  import JsProtocol._
  val inventoryManager : ActorRef
  val urlRoute =
    path("url" / "create") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "<sourceUrl>, <targetUrl>"))
      } ~
        post {
          decodeRequest {
            entity(as[UrlShorteningRequest]) { req => processUrlShortReq(req)
            }
          }
        }
    }
  
  def processUrlShortReq (req : UrlShorteningRequest ) : StandardRoute = {

    complete {
      println(s"Request received ${req.longUrl} ->   ${
        req.shortUrl match {
          case Some(s) => s
          case None    => "NONE"
        }
      }")
      implicit val timeout = Timeout(30 seconds)
      val future = inventoryManager ? req
      val result = Await.result(future, timeout.duration).asInstanceOf[InsertStatus]

      val respond = UrlShorteningRespond(result.status, req.longUrl,
        if (result.status)
          Option(result.message)
        else
          req.shortUrl)

      if (result.status == true) {
        createCommand(req.longUrl,
          Base62.decode(result.message),
          req.shortUrl match {
            case Some(s) => false
            case None    => true
          })
        HttpResponse(entity = respond.toJson.compactPrint)
      } else
        HttpResponse(StatusCodes.BadRequest, entity = respond.toJson.compactPrint)

    }
  }
  
  def createCommand(longUrl: String, shortUrlId: Long, random : Boolean) = {
    import JsProtocol._
    val command = UrlShorteningCommand(normalizeUrl(longUrl), shortUrlId, random)
    kafkaProducer.send(new ProducerRecord[String, String]("url.shortening.command", 
        (shortUrlId%readerNodeAddresses.size).toInt, "", 
        command.toJson.compactPrint))
  }
  
  def normalizeUrl (longUrl : String) : String =
    if ((longUrl.startsWith("http://")) || (longUrl.startsWith("https://"))) longUrl  
    else s"http://${longUrl}"
}
