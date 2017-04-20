package com.github.ngjiunnjye.shorturl.redirect.db

import java.sql.Connection

import scala.util.Try

trait CreateShortUrlTable {
  def createShortUrlTableIfNotExists(jdbcConn: Connection) = Try {
    jdbcConn.createStatement().execute("""
      create table if not exists Short_Url (
      |id  bigint primary Key,
      |long_Url varchar,
      |random boolean )
      """.stripMargin)
  }
}