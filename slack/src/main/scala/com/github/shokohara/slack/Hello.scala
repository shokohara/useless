package com.github.shokohara.slack

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import cats.derived.auto.eq._
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
import com.typesafe.scalalogging.LazyLogging
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.Interval.Closed
import io.chrisdavenport.cats.time._

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.chaining._

object Hello extends IOApp with LazyLogging {

  val config = IO(pureconfig.loadConfigOrThrow[ApplicationConfig])
  val zoneId = ZoneId.of("Asia/Tokyo")

  def toTimestampString(zonedDateTime: ZonedDateTime): String = (zonedDateTime.toInstant.toEpochMilli / 1000) + ".0000"

  def toEither[A <: SlackApiResponse, B, C](a: A, f: A => B, b: A => C): Either[C, B] = Either.cond(a.isOk, f(a), b(a))

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
      h <- g(slack, applicationConfig, c, ZonedDateTime.of(2019, 1, 31, 23, 59, 59, 0, zoneId), Nil).unsafeRunSync()
    } yield h
  }

  val slackCount: Int Refined Closed[W.`0`.T, W.`1000`.T] = 200

  def g(slack: Slack,
        applicationConfig: ApplicationConfig,
        c: Channel,
        until: ZonedDateTime,
        acc: List[Message]): IO[Either[RuntimeException, List[Message]]] =
    IO.fromEither(Try(
        ChannelsHistoryRequest
          .builder().token(applicationConfig.slackToken).channel(c.getId).count(slackCount.value)
          .pipe { b =>
            logger.debug(acc.length.show)
            acc.toNel.fold(b)(nel =>
              b.latest(toTimestampString(nel.map(_.ts).minimum)).oldest(
                  toTimestampString(nel.map(_.ts).minimum.minusDays(1))))
          }
          .build().tap(b => logger.debug(b.toString)).pipe(slack.methods().channelsHistory)
          .pipe(a =>
            toEither(a: ChannelsHistoryResponse,
                     (_: ChannelsHistoryResponse).getMessages,
                     (_: ChannelsHistoryResponse).getError.pipe(new RuntimeException(_))))
          .map(_.asScala.toList.map(m2m))
    ).toEither).flatMap { h =>
        h.fold(
          a => IO.pure(a.asLeft),
          h => {
            logger.debug(h.length.show)
            h.toNel.fold[IO[Either[RuntimeException, List[Message]]]](IO.pure(acc.asRight))(
              h =>
                if (h.forall(a => acc.nonEmpty && acc.exists(m => m === a)))
                  IO.pure(new RuntimeException("レスポンスボディの要素が重複しました").asLeft)
                else {
                  h.nonEmptyPartition(zdt => {
//                  logger.debug((zdt.ts < until).show)
                      if (zdt.ts < until) zdt.asLeft else zdt.asRight
                    }).fold(
                      left => {
//                    logger.debug(left.map(_.ts).map(_.show).mkString_("","\n",""))
                        IO.pure(acc.asRight)
                      },
                      n => {
                        logger.debug((n ++ acc).map(_.ts).maximum.show)
                        logger.debug((n ++ acc).map(_.ts).minimum.show)
                        logger.debug("g")
                        g(slack, applicationConfig, c, until, acc ++ n.toList)
                      },
                      (left, right) => {
                        logger.debug(left.map(_.ts).map(_.show).mkString_("", "\n", ""))
                        logger.debug(right.map(_.ts).map(_.show).mkString_("", "\n", ""))
                        IO.pure((acc ++ right.toList).asRight)
                      }
                    )
              })
          }
        )
      }

  def m2m(a: com.github.seratch.jslack.api.model.Message): Message =
    slack.Message(
      a.getUser, {
//        println(a.getTs)
        val b = a.getTs.split('.')
//        println(b.toList)
        // ミリ秒が欠如してる
        // テストする
        ZonedDateTime.from(Instant.ofEpochSecond(b.head.toLong).atOffset(ZoneOffset.UTC))
      },
      a.getText
    )

  def run(args: List[String]): IO[ExitCode] =
    config.flatMap(f).flatMap(_.fold(IO.raiseError, _.traverse_(m => IO.pure(logger.info(m.ts.show))))).map { _ =>
//    IO {
//      println(ZonedDateTime.now(ZoneOffset.UTC))
//      println(ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(zoneId))
//      println(ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime)
      ExitCode.Success
    }
}
