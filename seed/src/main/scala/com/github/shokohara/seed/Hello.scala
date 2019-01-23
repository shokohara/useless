package com.github.shokohara.seed

object Hello extends Greeting with App {
  println(greeting)
}

trait Greeting {
  lazy val greeting: String = "hello"
}
