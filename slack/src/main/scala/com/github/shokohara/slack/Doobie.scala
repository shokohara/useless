package com.github.shokohara.slack

import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect._
import doobie.imports._

import scala.concurrent.ExecutionContext

object Doobie {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    "jdbc:postgresql://127.0.0.1:5432/slack", // connect URL (driver-specific)
    "sho", // user
    "" // password
  )
}

case class SlackModel(teamName: String, channelName: String, userId: String, text: String, ts: ZonedDateTime)

object SlackModel {

  implicit val InstantMeta: Meta[Instant] =
    Meta[Timestamp].xmap(_.toInstant, Timestamp.from)

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] = {
    val utc = ZoneId.of("UTC")
    Meta[Instant].xmap(ZonedDateTime.ofInstant(_, utc), Instant.from)
  }

  val drop =
    sql"""
    DROP TABLE IF EXISTS slack
  """.update.run

  val create =
    sql"""
    CREATE TABLE slack (
      teamName       VARCHAR NOT NULL,
      channelName    VARCHAR NOT NULL,
      userId         VARCHAR NOT NULL,
      text           VARCHAR NOT NULL,
      ts             TIMESTAMP WITH TIME ZONE NOT NULL,
      UNIQUE (teamName, channelName, userId, text, ts)
    )
  """.update.run

  def insert1(slackModel: SlackModel): Update0 =
    sql"insert into slack (teamName, channelName, userId, text, ts) values (${slackModel.teamName}, ${slackModel.channelName}, ${slackModel.userId}, ${slackModel.text}, ${slackModel.ts})".update
}
