package com.github.shokohara.slack

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import cats.effect._
import cats.implicits._
import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.SlackApiResponse
import com.github.seratch.jslack.api.methods.request.channels._
import com.github.seratch.jslack.api.methods.request.users.UsersListRequest
import com.github.seratch.jslack.api.methods.response.channels._
import com.github.seratch.jslack.api.methods.response.users.UsersListResponse
import com.github.seratch.jslack.api.model.Channel
import com.github.shokohara.slack
import eu.timepit.refined.auto._
import io.chrisdavenport.cats.time._

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.chaining._

object Hello extends IOApp {

  val config = IO(pureconfig.loadConfigOrThrow[ApplicationConfig])

//  def toTimestampString(dateTime: DateTime): String = dateTime.getMillis + ".0000

  def toEither[A <: SlackApiResponse, B, C](a: A, f: A => B, b: A => C) = Either.cond(a.isOk, f(a), b(a))

  def f(applicationConfig: ApplicationConfig): IO[Either[RuntimeException, List[Message]]] = IO {
    val slack: Slack = Slack.getInstance
    for {
      u <- UsersListRequest
        .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().usersList)
        .pipe(a =>
          toEither(a: UsersListResponse,
                   (_: UsersListResponse).getMembers,
                   (_: UsersListResponse).getError.pipe(new RuntimeException(_))))
        .flatMap(
          a =>
            a.asScala
              .filter(_.getName === applicationConfig.slackUserName).toList.toNel // refined 1
              .map(_.head)
              .toRight(new RuntimeException(""))) // getRealNameかも。重複しないのはどっち？ // 1の長さのリスト
      c <- ChannelsListRequest
        .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().channelsList).pipe(a =>
          toEither(a: ChannelsListResponse,
                   (_: ChannelsListResponse).getChannels,
                   (_: ChannelsListResponse).getError.pipe(new RuntimeException(_)))).flatMap(
          _.asScala.find(_.getName === applicationConfig.slackChannelName).toRight(new RuntimeException("")))
      h <- g(slack, applicationConfig,c,ZonedDateTime.of(2019,1,31,23,59,59,0,ZoneId.of("Asia/Tokyo")) ,Nil).unsafeRunSync()
    } yield h
  }
  def g(slack: Slack,
        applicationConfig: ApplicationConfig,
        c: Channel,
        until: ZonedDateTime,
        acc: List[Message]): IO[Either[RuntimeException, List[Message]]] =
    IO.fromEither(Try(ChannelsHistoryRequest
      .builder().token(applicationConfig.slackToken).channel(c.getId).build().pipe(slack.methods().channelsHistory)
      .pipe(a =>
        toEither(a: ChannelsHistoryResponse,
                 (_: ChannelsHistoryResponse).getMessages,
                 (_: ChannelsHistoryResponse).getError.pipe(new RuntimeException(_))))
      .map(_.asScala.toList.map(m2m))).toEither)
      .flatMap(
        h =>h.fold(a=>IO.pure(a.asLeft),h=>
          h.toNel.fold[IO[Either[RuntimeException, List[Message]]]](IO.pure(acc.asRight))(
            nelH =>
              nelH
                .nonEmptyPartition(zdt => if (zdt.ts < until) zdt.asRight else zdt.asLeft).fold(
                  _ => IO.pure(acc.asRight),
                  n => g(slack, applicationConfig, c, until, acc ++ n.toList),
                  (_, right) => IO.pure((acc ++ right.toList).asRight)
              ))))

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

  def run(args: List[String]): IO[ExitCode] =
    config
      .flatMap(f).flatMap(_.fold(IO.raiseError, _.traverse_(_.toString.pipe(a => IO(println(a)))))).map(_ =>
        ExitCode.Success)
}
