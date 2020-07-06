package com.github.shokohara.lol

import cats.implicits._
import com.merakianalytics.orianna.Orianna
import com.merakianalytics.orianna.types.common.Region
import com.merakianalytics.orianna.types.core.`match`.Match
import com.merakianalytics.orianna.types.core.spectator.Player
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.jdk.CollectionConverters._

final case class ApplicationConfig(riotApiKey: String)

object Main extends App {

  val summonerName = ""

  for {
    config <- ConfigSource.default.load[ApplicationConfig]
    _ = Orianna.setRiotAPIKey(config.riotApiKey)
    _ = Orianna.setDefaultRegion(Region.JAPAN)
    summoner = Orianna.summonerNamed(summonerName).get
    currentMatch = Orianna.currentMatchForSummoner(summoner).get()
//    mySide =
//      currentMatch.getParticipants.asScala
//        .find(_.getSummoner.getAccountId === summoner.getAccountId)
//        .toRight(new RuntimeException("currentMatch.participantsに自分のaccountIdが存在しないため、自分のチームが不明でした。"))
//        .map(_.getTeam.getSide)
    playerKeyMatchHistoryValueMap =
      currentMatch.getParticipants.asScala
        .map(p => (p, Orianna.matchHistoryForSummoner(p.getSummoner).get().asScala)).toMap
  } yield playerKeyMatchHistoryValueMap.map {
    case (p, matchHistory) =>
      (
        p,
        playerKeyMatchHistoryValueMap.keys
          .filter(_.getSummoner.getAccountId != p.getSummoner.getAccountId)
          .flatMap(playerKey =>
            matchHistory
              .filter(
                _.getParticipants.asScala.exists(_.getSummoner.getAccountId === playerKey.getSummoner.getAccountId)
              )
          )
      )
  }: Map[Player, Iterable[Match]]
}
