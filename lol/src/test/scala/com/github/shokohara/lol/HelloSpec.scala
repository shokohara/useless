package com.github.shokohara.lol

import org.scalatest._

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    Hello.greeting shouldEqual "hello"
  }
}
