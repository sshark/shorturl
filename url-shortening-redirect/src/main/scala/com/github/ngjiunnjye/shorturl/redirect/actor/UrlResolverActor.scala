package com.github.ngjiunnjye.shorturl.redirect.actor

import java.sql.{DriverManager, PreparedStatement}

import akka.actor.{Actor, actorRef2Scala}
import com.github.ngjiunnjye.cryptor.Base62
import com.github.ngjiunnjye.shorturl.redirect.db.CreateShortUrlTable
import com.github.ngjiunnjye.shorturl.utils.Config

import scala.util.{Failure, Success, Try}


case class QueryStatus (status : Boolean, message : String )

class UrlResolverActor extends Actor with Config with CreateShortUrlTable{
  
  val jdbcConn = {
    Class.forName("org.h2.Driver");
    DriverManager.getConnection(s"jdbc:h2:tcp://${h2ServerUrls.get(nodeId)}", "", "");
  }
    
  val queryPs: PreparedStatement = {
    Try {
      createShortUrlTableIfNotExists (jdbcConn)

      jdbcConn.prepareStatement("select long_url from short_url where id = ?")
      
    } match {
      case Success(ps) => ps
      case Failure(e) => throw e
    }     
  }
  private var maxId: Long = _
  
  def receive = {
    case shortUrl : String => sender ! processUrlResolver (shortUrl)
    case _       => println("unknown Request")
  }
  
  def processUrlResolver (shortUrl : String) = {
    Base62.decode(shortUrl).flatMap(code => {
      queryPs.setLong(1,code)

        queryPs.executeQuery()
      } match{
        case Success(rs) =>
          if (rs.next()) QueryStatus(true, rs.getString(1))
          else QueryStatus(false, s"Url ${shortUrl} not found")
          
          
        case Failure(e) => QueryStatus(false, e.getMessage)
      }
  }
}