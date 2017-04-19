package com.github.ngjiunnjye.cryptor

import scala.util.{Failure, Success, Try}

object Base62 {
  private val base62Dict = (('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')).toSeq
  private val base62Size = base62Dict.size

  def encode(num: Long): String = {
    def loop(buf: String, left: Long): String = {
      if (left < 62L) base62Dict((left % base62Size).toInt) + buf
      else {
        loop(base62Dict((left % base62Size).toInt) + buf, (left / base62Size))
      }
    }

    loop("", num)
  }

  def decode(encoded: String, sum: Long = 0): Try[Long] =
    if (encoded.isEmpty) Success(sum)
    else {
      if (base62Dict.indexOf(encoded.head) == -1) Failure(new Exception(s"Unsupported Character ${encoded.head} for base62 mapper"))
      else decode(encoded.tail, sum + scala.math.pow(base62Size, encoded.tail.size).toLong * base62Dict.indexOf(encoded.head))
    }
}