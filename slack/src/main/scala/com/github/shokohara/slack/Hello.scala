package com.github.shokohara.slack

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import cats.data.{EitherT, OptionT}
import cats.effect._
import cats.implicits._
import pureconfig.generic.auto._
import com.github.seratch.jslack._
import com.github.seratch.jslack.api.methods.request.channels._
import com.github.seratch.jslack.api.methods.request.users.UsersListRequest
import com.github.seratch.jslack.api.methods.response.channels._

import collection.JavaConverters._
import cats.implicits._
import com.github.seratch.jslack.api.methods.SlackApiResponse
import com.github.seratch.jslack.api.methods.response.users.UsersListResponse
import com.github.seratch.jslack.api.model.Message
import com.github.shokohara.slack
import shapeless.nat._
import eu.timepit.refined.collection.Size
import eu.timepit.refined._
import eu.timepit.refined.collection._
import com.github.seratch.jslack.Slack
import scala.util.chaining._
import eu.timepit.refined._

object Hello extends IOApp {

  val config = IO(pureconfig.loadConfigOrThrow[ApplicationConfig])

//  def toTimestampString(dateTime: DateTime): String = dateTime.getMillis + ".0000

  def toEither[A <: SlackApiResponse, B, C](a: A, f: A => B, b: A => C) = Either.cond(a.isOk, f(a), b(a))

  def f(applicationConfig: ApplicationConfig): IO[Either[RuntimeException, List[Message]]] = IO {
    val slack: Slack = Slack.getInstance
    for {
      u <- UsersListRequest
        .builder().token(applicationConfig.token).build().pipe(slack.methods().usersList)
        .pipe(a =>
          toEither(a: UsersListResponse,
                   (_: UsersListResponse).getMembers,
                   (_: UsersListResponse).getError.pipe(new RuntimeException(_))))
        .flatMap(
          _.asScala
            .filter(_.getName === applicationConfig.userName).toList.toNel // refined 1
            .map(_.head)
            .toRight(new RuntimeException(""))
        ) // getRealNameかも。重複しないのはどっち？ // 1の長さのリスト
      c <- ChannelsListRequest
        .builder().token(applicationConfig.token).build().pipe(slack.methods().channelsList).pipe(a =>
          toEither(a: ChannelsListResponse,
                   (_: ChannelsListResponse).getChannels,
                   (_: ChannelsListResponse).getError.pipe(new RuntimeException(_)))).flatMap(
          _.asScala.find(_.getName === "random").toRight(new RuntimeException("")))
      h <- ChannelsHistoryRequest
        .builder().token(applicationConfig.token).channel(c.getId).build().pipe(slack.methods().channelsHistory)
        .pipe(a =>
          toEither(a: ChannelsHistoryResponse,
                   (_: ChannelsHistoryResponse).getMessages,
                   (_: ChannelsHistoryResponse).getError.pipe(new RuntimeException(_))))

    } yield h.asScala.toList.map(m2m)
  }

  def m2m(a: com.github.seratch.jslack.api.model.Message): Message =
    slack.Message(
      a.getUser, {
        println(a.getTs)
        val b = a.getTs.split('.')
        println(b.toList)
        // ミリ秒が欠如してる
        ZonedDateTime.from(Instant.ofEpochSecond(b.head.toLong).atOffset(ZoneOffset.UTC))
      },
      a.getText
    )

  def run(args: List[String]): IO[ExitCode] = config.flatMap(f).map(_ => ExitCode.Success)
}
