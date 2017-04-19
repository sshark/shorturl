package com.github.ngjiunnjye.shorturl.redirect.actor

import java.sql.DriverManager

import akka.actor.Actor
import com.github.ngjiunnjye.kafka.{Consumer => KafkaConsumer}
import com.github.ngjiunnjye.shorturl.redirect.db.CreateShortUrlTable
import com.github.ngjiunnjye.shorturl.utils.JsProtocol.commandFormat
import com.github.ngjiunnjye.shorturl.utils.{Config, UrlShorteningCommand}
import org.apache.kafka.common.TopicPartition
import spray.json.JsonParser
import spray.json.ParserInput.apply

import scala.collection.JavaConversions.{iterableAsScalaIterable, seqAsJavaList}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Try


class KafkaToDbWriteActor extends Actor with Config with CreateShortUrlTable {
  val jdbcConn = {
    Class.forName("org.h2.Driver");
    DriverManager.getConnection(s"jdbc:h2:tcp://${h2ServerUrls.get(nodeId)}", "", "");
  }

  val insertPS = Try {
    createShortUrlTableIfNotExists(jdbcConn)
    jdbcConn.prepareStatement("insert into short_Url (id, long_url, random) values (?,?, ?)")
  }

  val consumer = KafkaConsumer.createStringStringConsumer(s"redirect")
  val topic: String = "url.shortening.command";
  val partition: TopicPartition = new TopicPartition(topic, nodeId);
  consumer.assign(List(partition));
  final val POLL_TIMOUT_MS = 1000
  println("KafkaToDbWriteActor started ")

  def receive = {
    case "FetchKafkaMessage" =>
      val fetchSize = fetch
      if (fetchSize > 0) consumer.commitSync

  }

  override def preStart() = {
    scheduleFetchMessage
  }

  override def postStop(): Unit = consumer.close

  def fetch = {
    val records = consumer.poll(POLL_TIMOUT_MS)
    records.foreach { record =>
      val kafkaMsg = record.value
      //TODO commit after write val offset = record.offset()
      JsonParser(kafkaMsg).convertTo[UrlShorteningCommand] match {
        case cmd: UrlShorteningCommand =>
          insert(cmd)
        case _ => println(s"????")
      }
    }
    scheduleFetchMessage
    records.size
  }

  def scheduleFetchMessage =
    context.system.scheduler.scheduleOnce(1.second, self, "FetchKafkaMessage")

  def close: Unit = consumer.close()

  def insert(cmd: UrlShorteningCommand) =
    insertPS.map(ps => {
      ps.setLong(1, cmd.shortUrlId)
      ps.setString(2, cmd.longUrl)
      ps.setBoolean(3, cmd.random)
      ps.executeUpdate()
    })

  /*
  match{
    case Success(s) =>
      println(s"ShortUrl Entry Created ID: id:${cmd.shortUrlId} longUrl:${cmd.longUrl} random:${cmd.random}")

    case Failure(e) =>
      println(s"ShortUrl Entry Fail ID: id:${cmd.shortUrlId} longUrl:${cmd.longUrl} random:${cmd.random} ${e.getMessage}")

  }
  */

}