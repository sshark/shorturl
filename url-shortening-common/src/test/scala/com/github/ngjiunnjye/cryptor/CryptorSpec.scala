package com.github.ngjiunnjye.cryptor

import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}


class CryptorSpec extends WordSpec with Matchers {

  "Test Encode Decode 1" in {
    val encode = Base62.encode(1)
    Base62.decode(encode).foreach(_ should be(1))
  }

  "Test Decode Encode 0" in {
    val short = "0"
    Base62.decode(short).foreach(l => {
      l should be(0L)
      Base62.encode(l) should be(short)
    })
  }

  "Test Decode Encode 1" in {
    val short = "1"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(1L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode a" in {
    val short = "a"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(10L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode A" in {
    val short = "A"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(36L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode Z" in {
    val short = "Z"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(61L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode 10" in {
    val short = "10"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(62L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode 20" in {
    val short = "20"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(124L))
    decode.foreach(Base62.encode(_) should be(short))
  }


  "Test Decode Encode 100" in {
    val short = "100"
    val decode = Base62.decode(short)
    decode.foreach(_ should be(3844L))
    decode.foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode google" in {
    val short = "google"
    Base62.decode(short).foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode Google" in {
    val short = "Google"
    Base62.decode(short).foreach(Base62.encode(_) should be(short))
  }

  "Test Decode Encode Google is not google" in {
    Base62.decode("Google").foreach(Base62.encode(_) should not be "google")
  }

  "Test Decode Encode underscore" in {

    Base62.decode("____") match {
      case Success(s) =>
        s"decode ___" should not be (s)
      case Failure(e) =>
        e.getMessage should be("Unsupported Character _ for base62 mapper")
    }
  }
}