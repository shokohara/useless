package com.github.shokohara.lol

import cats.effect._
import net.rithms.riot.api.{ApiConfig, RiotApi}
import net.rithms.riot.constant.Platform.JP

import scala.concurrent.ExecutionContext

object Hello extends Greeting {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  def program2(args: List[String]): IO[Unit] = IO {
    val token = sys.env("LOL_TOKEN")
    val summonerName = sys.env("LOL_NAME")
    val config = new ApiConfig().setKey(token)
    val api = new RiotApi(config)
    val summoner = api.getSummonerByName(JP, summonerName)
    val accountId = summoner.getAccountId
    val summonerId = summoner.getId

//    println(api)
    println(api.getSummoner(JP, summonerId))
    println(api.getMatchListByAccountId(JP, accountId).getMatches)

//    println(api.getActiveGameBySummoner(JP, summonerId))
//        val summonerIds = v.getParticipants.asScala.map(_.getSummonerId)
//        println(summonerIds)
  }

  def main(args: Array[String]): Unit =
    program2(args.toList).unsafeRunSync
}

trait Greeting {
  lazy val greeting: String = "hello"
}
