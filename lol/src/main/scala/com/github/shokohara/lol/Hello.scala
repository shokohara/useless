package com.github.shokohara.lol

import cats.effect._
import cats.syntax.apply._
import com.merakianalytics.orianna.Orianna
import com.merakianalytics.orianna.types.common.{Region, Side}
import com.merakianalytics.orianna.types.core.staticdata.Champion

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Hello extends Greeting {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  def program(args: List[String]): IO[Unit] = IO {
    val token = sys.env("LOL_TOKEN")
    val summonerName = sys.env("LOL_NAME")
    Orianna.setRiotAPIKey(token)
    Orianna.setDefaultRegion(Region.JAPAN)
    val summoner = Orianna.summonerNamed("でーし").get()
    val cm = Orianna.currentMatchForSummoner(summoner).get()
    for {
      participants <- Option(cm.getParticipants)
      myPlayer <- participants.asScala.find(_.getSummoner.getAccountId == summoner.getAccountId)
      mySide: Side = myPlayer.getTeam.getSide
      (_, theirParticipants) = cm.getParticipants.asScala.partition(_.getTeam.getSide == mySide)
    } yield println(theirParticipants.map(_.getChampion).map(a).mkString("-----------------------"))
  }

  def a(c: Champion): String =
    s"""${c.getName}
       |CD: ${c.getSpells.asScala.map(_.getCooldowns.asScala.map(a => b(a)).mkString("/")).mkString("\n")}
       |CD: ${c.getSpells.asScala.map(_.getRanges.asScala)}
       |""".stripMargin

  def b(a: Double): String = if (a % 1 == 0) a.toInt.toString else a.toString

  def program2(args: List[String]): IO[Unit] = IO {
    //    val token = sys.env("LOL_TOKEN")
    //    val summonerName = sys.env("LOL_NAME")
    //    val config = new ApiConfig().setKey(token)
    //    val api = new RiotApi(config)
    //    val summoner = api.getSummonerByName(JP, summonerName)
    //    val accountId = summoner.getAccountId
    //    val summonerId = summoner.getId
    //
    ////    println(api)
    //    println(api.getSummoner(JP, summonerId))
    //    println(api.getMatchListByAccountId(JP, accountId).getMatches)
    //    println(Try(api.getActiveGameBySummoner(JP, summonerId)))
    //    println(Try(api.getChampionMasteryScoresBySummoner(JP,summonerId)))
    //    println(Try(api.getChampionMasteriesBySummoner(JP,summonerId)))
//        val summonerIds = v.getParticipants.asScala.map(_.getSummonerId)
//        println(summonerIds)
    ()
  }

  def main(args: Array[String]): Unit = (program(args.toList) *> program2(args.toList)).unsafeRunSync
}

trait Greeting {
  lazy val greeting: String = "hello"
}
