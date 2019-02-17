package com.github.shokohara.lol
import cats.effect.{IO, Timer}
import com.merakianalytics.orianna.Orianna
import com.merakianalytics.orianna.types.common.Region

import scala.concurrent.ExecutionContext

object Test {

  def main(args: Array[String]): Unit = {
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    val token = sys.env("RIOT_TOKEN")
    Orianna.setRiotAPIKey(token)
    Orianna.setDefaultRegion(Region.JAPAN)
  }
}
