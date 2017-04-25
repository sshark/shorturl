package com.github.ngjiunnjye.shorturl.creator.actor

import com.github.ngjiunnjye.kafka.{Consumer => KafkaConsumer, Producer => KafkaProducer}
import com.github.ngjiunnjye.shorturl.utils.{JsProtocol, UrlShorteningRequest}
import org.apache.kafka.clients.producer.ProducerRecord
import spray.json.DefaultJsonProtocol._
import spray.json.pimpAny


case class InventorySnapshot (lastRequestTime : Long, maxId : Long, preferedList : List [Long])

trait InventoryJournal {
  val journalTopic = "url.shortening.request"
  val snapShotTopic = "url.shortening.request-snapshot"
  var snapshotLastReqTime : Long = _
  final val POLL_TIMOUT_MS = 1000

  val kafkaProducer = KafkaProducer.createLongStringProducer
  implicit val snapShotFormat = jsonFormat3(InventorySnapshot)

  val consumer = KafkaConsumer.createLongStringConsumer(s"journal")
  

  def createRequestJournal(req: UrlShorteningRequest) = {
    import JsProtocol._
    snapshotLastReqTime = System.currentTimeMillis() 
    kafkaProducer.send(new ProducerRecord[Long, String](journalTopic,
    snapshotLastReqTime  , req.toJson.compactPrint))
  }

  def createRequestSnapShot(req: InventorySnapshot) = {
    kafkaProducer.send(new ProducerRecord[Long, String](snapShotTopic,
      snapshotLastReqTime, req.toJson.compactPrint))
  }
}