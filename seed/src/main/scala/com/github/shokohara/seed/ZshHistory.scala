package com.github.shokohara.seed

import java.nio.file.Paths

import scala.collection.JavaConverters._

object ZshHistory extends App {
  val file = java.nio.file.Files.readAllLines(Paths.get("/Users/sho/Documents/environment/.zsh_history")).asScala
  //  val file = java.nio.file.Files.readAllLines(Paths.get("/Users/sho/Documents/environment/zsh_history")).asScala
  file.take(1).map { x =>
    println(x)
    val m = """\s([0-9]+):([0-9]+);([\w\s~/.]*)""".r.findAllIn(x)
    val snapshot =  m.group(1)
    val executionTime = m.group(2)
    val sh = m.group(3)
    assert(x.dropWhile(_ != ';').drop(1) == sh)
  }
}
