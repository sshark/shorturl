package com.github.ngjiunnjye.shorturl.redirect.actor

import java.sql.{Connection, DriverManager, PreparedStatement}

import akka.actor.{Actor, actorRef2Scala}
import com.github.ngjiunnjye.cryptor.Base62
import com.github.ngjiunnjye.shorturl.redirect.db.CreateShortUrlTable
import com.github.ngjiunnjye.shorturl.utils.Config

import scala.util.Try

case class QueryStatus(status: Boolean, message: String)

class UrlResolverActor extends Actor with Config with CreateShortUrlTable {

  val jdbcConn = Try {
    Class.forName("org.h2.Driver");
    DriverManager.getConnection(s"jdbc:h2:tcp://${h2ServerUrls.get(nodeId)}", "", "");
  }

  def queryPs(conn: Connection): Try[PreparedStatement] = Try {
    createShortUrlTableIfNotExists(conn)
    conn.prepareStatement("select long_url from short_url where id = ?")
  }

  private var maxId: Long = _

  def receive = {
    case shortUrl: String => sender ! processUrlResolver(shortUrl)
    case _ => println("unknown Request")
  }

  def processUrlResolver(shortUrl: String) = for {
    conn <- jdbcConn
    ps <- queryPs(conn)
    code <- Base62.decode(shortUrl)
    shortCode <-  retrieveStringCode(ps, code)
  } yield shortCode

  def retrieveStringCode(ps: PreparedStatement, code: Long) = {
      ps.setLong(1, code)
      val rs = ps.executeQuery()
      if (rs.next()) Some(rs.getString(1))
      else None
  }
}