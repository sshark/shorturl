package com.github.ngjiunnjye.shorturl.creator.actor

import akka.actor.{Actor, actorRef2Scala}
import com.github.ngjiunnjye.cryptor.Base62
import com.github.ngjiunnjye.shorturl.utils.JsProtocol.requestFormat
import com.github.ngjiunnjye.shorturl.utils.{Config, UrlShorteningRequest}
import spray.json.JsonParser
import spray.json.ParserInput.apply

import scala.collection.JavaConversions.{iterableAsScalaIterable, seqAsJavaList}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Try

case class InsertStatus(message: Option[String])

class InventoryManagerActor extends Actor with Config  with InventoryJournal {
  
  private var maxId: Long = _
  val preferedList = new ListBuffer[Long]
  
  def getLastSnapshot : InventorySnapshot  = {
    consumer.subscribe(List(snapShotTopic))

    val records = consumer.poll(POLL_TIMOUT_MS)
    consumer.unsubscribe()
    if (records.isEmpty())
        InventorySnapshot(0, 0, List())
    else {   
      println (s" last snapshot ${records.last.value()}")
      JsonParser(records.last.value()).convertTo[InventorySnapshot]
    }
  }

  def journalPlayback  = {
    val invSnapShot = getLastSnapshot
    val snapShotTime = invSnapShot.lastRequestTime
    println ("getLastSnapshot")
    maxId = invSnapShot.maxId
    preferedList ++= invSnapShot.preferedList
    
    consumer.subscribe(List(journalTopic))
    val records = consumer.poll(POLL_TIMOUT_MS)
    println (s"journalPlayback ${records.size}")
    records.foreach { record =>
      Option(record.key()).filter(_ > snapShotTime).foreach(_ =>
        processCreateRequest(JsonParser(record.value).convertTo[UrlShorteningRequest]))
    }
    consumer.close()
  }

  override def preStart() = {
    scheduleSnapshotMessage
    journalPlayback
    println (s"maxId ${maxId}")
    println (s"preferedList ${preferedList.mkString(",")}")
  }

  def receive = {
    case req: UrlShorteningRequest => {
      createRequestJournal(req)
      sender ! processCreateRequest(req)
    }
    case "Snapshot" => {
      createRequestSnapShot(InventorySnapshot(snapshotLastReqTime, maxId, preferedList.toList))
      scheduleSnapshotMessage
    }
    case _ => println("unknown Request")
  }

  def processCreateRequest(req: UrlShorteningRequest) =
    req.shortUrl.map(target => processCreateRequestPreferred(req.longUrl, target))
      .getOrElse(processCreateRequestRandom(req.longUrl))

  def processCreateRequestPreferred(longUrl: String, shortUrl: String): Try[InsertStatus] =
    Base62.decode(shortUrl).map(code =>
      if (preferedList.contains(code)) {
        println(s"${shortUrl} not available")
        InsertStatus(None)
      } else {
        preferedList += code
        InsertStatus(Some(Base62.encode(code)))
      })

  def processCreateRequestRandom(longUrl: String): InsertStatus =
    InsertStatus(Some(Base62.encode(getNextId)))

  def getNextId: Long = {
    maxId += 1
    if (preferedList.contains(maxId)) getNextId
    else maxId
  }

  def scheduleSnapshotMessage =
    context.system.scheduler.scheduleOnce(1.minute, self, "Snapshot")
}