package com.github.shokohara.lol

import cats.data._
import cats.effect._
import cats.implicits._
import com.merakianalytics.orianna.Orianna
import com.merakianalytics.orianna.types.common.{Region, Side}
import com.merakianalytics.orianna.types.core.spectator.{CurrentMatch, Player}
import com.merakianalytics.orianna.types.core.staticdata.{Champion, ChampionSpell}
import com.merakianalytics.orianna.types.core.summoner.Summoner

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Hello extends Greeting {

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  val token = sys.env("RIOT_TOKEN")
  Orianna.setRiotAPIKey(token)
  Orianna.setDefaultRegion(Region.JAPAN)
  val summoner: Summoner = Orianna.summonerNamed("でーし0").get()
  val cm: CurrentMatch = Orianna.currentMatchForSummoner(summoner).get()
  val theirParticipants: Option[NonEmptyList[Player]] = for {
    participants <- Option(cm.getParticipants)
    myPlayer <- participants.asScala.find(_.getSummoner.getAccountId == summoner.getAccountId)
    mySide: Side = myPlayer.getTeam.getSide
    r <- cm.getParticipants.asScala.filter(_.getTeam.getSide != mySide).toList.toNel
  } yield r
  val theirChampions: NonEmptyList[Champion] = theirParticipants.get.map(_.getChampion).toList.toNel.get

  val program1: IO[Unit] = IO {
    println(theirChampions)
//    theirChampions.zipWithIndex.foreach(println)
  }

//  val program2: IO[Unit] = IO {
//    println(theirChampions.map(a).mkString(""))
//  }

  val program3: IO[Int] = IO {
    val a = readInt()
    a.toInt
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
  }

  def program4(i: Int): IO[Unit] = IO {
    println(a(theirChampions.toList(i)))
  }

  def program5(i: Int): IO[Unit] = IO {
    println(a(theirChampions.toList(i)))
  }

  val lb = "\n"

  def a(a: Champion): String =
    s"""${a.getName}
      |${a.getPassive.description()}
      |${a.getSpells.asScala.map(cs).mkString(lb+ lb)}
      |
    """.stripMargin

  def cd(a: Double): String = if (a % 1 == 0) a.toInt.toString else a.toString

  def cs(a: ChampionSpell): String =
    a.getName + lb + (if (a.getRanges.asScala.distinct.length == 1) {
      a.getDescription + lb +
        "CD:    " + a.getCooldowns.asScala.map(cd(_)).mkString("/") + lb + "RANGE: " + a.getRanges.asScala.head.toInt.toString
    } else (a.getRanges.asScala zip a.getCooldowns.asScala).map({ case (range, c) =>
      "CD:    " + cd(c) + lb + "RANGE: " + range.toInt.toString
    }).mkString(""))

  def d(a: ChampionSpell): String =
    a.getDescription


  def main(args: Array[String]): Unit = (program1 *> program3.flatMap(program4)).unsafeRunSync
}

trait Greeting {
  lazy val greeting: String = "hello"
}
