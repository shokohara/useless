package com.github.shokohara.lol

import cats.Show
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
import scala.io.StdIn
import scala.util.Properties

object Hello extends Greeting {

  val summoner: IO[Summoner] = IO(Orianna.summonerNamed("でーし0").get())

  def currentMatch(a: Summoner): IO[CurrentMatch] = IO.fromEither {
    val cm = Orianna.currentMatchForSummoner(a).get()
    Option(cm.getParticipants).toRight(new RuntimeException("currentMatchForSummoner returns empty.")).map(_ => cm)
  }
  val champ: IO[Champion] = IO(Orianna.getChampions.get(1))
  val readInt: IO[Int] = IO(StdIn.readInt())
  val lb: String = Properties.lineSeparator

  def program1(i: Option[Int]): IO[Unit] =
    summoner.flatMap(s => currentMatch(s).map((_, s))).flatMap {
      case (cm, s) =>
        IO.fromEither(
          for {
            participants <- Option(cm.getParticipants).toRight(new RuntimeException(""))
            myPlayer <- participants.asScala
              .find(_.getSummoner.getAccountId == s.getAccountId).toRight(new RuntimeException(""))
            mySide: Side = myPlayer.getTeam.getSide
            theirParticipants <- cm.getParticipants.asScala
              .filter(_.getTeam.getSide != mySide).toList.toNel
              .toRight(new RuntimeException("toNel"))
            theirChampions <- theirParticipants.map(_.getChampion).toList.toNel.toRight(new RuntimeException())
          } yield
            i.fold(theirChampions.map(_.getName).zipWithIndex.toList.foreach(println))(i =>
              println(theirChampions.toList(i).show)))
    }

  implicit val championShow: Show[Champion] = (a: Champion) => s"""${a.getName}
       |
       |${a.getPassive.getName}
       |${a.getPassive.description()}
       |
       |${a.getSpells.asScala.map(_.show).mkString(lb)}
       |
    """.stripMargin

  implicit val championSpellShow: Show[ChampionSpell] = (a: ChampionSpell) =>
    a.getName + lb + (if (a.getRanges.asScala.distinct.length == 1) {
                        s"""${a.getDescription}
         |CD:    ${if (a.getCooldowns.asScala.distinct.length == 1)
                             a.getCooldowns.asScala.map(showCoolDown(_)).mkString("/")}
         |RANGE: ${a.getRanges.asScala.head.toInt.toString}
      """.stripMargin
                      } else
                        (a.getRanges.asScala zip a.getCooldowns.asScala)
                          .map({
                            case (range, c) =>
                              s"""
               |CD:    ${showCoolDown(c)}
               |RANGE: ${range.toInt.toString}
      """.stripMargin
                          }).mkString(""))

  def showCoolDown(a: Double): String = if (a % 1 === 0) a.toInt.toString else a.toString

  def main(args: Array[String]): Unit = {
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    val token: String = sys.env("RIOT_TOKEN")
    Orianna.setRiotAPIKey(token)
    Orianna.setDefaultRegion(Region.JAPAN)
    (program1(None) *> readInt.map(_.some).flatMap(program1)).unsafeRunSync
  }
}

trait Greeting {
  lazy val greeting: String = "hello"
}
