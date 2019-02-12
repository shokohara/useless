package com.github.shokohara.slack

import java.time._

import cats.data.{NonEmptyChain, NonEmptyList, ValidatedNel}
import cats.derived.auto.eq._
import cats.effect._
import cats.implicits._
import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.SlackApiResponse
import com.github.seratch.jslack.api.methods.request.channels._
import com.github.seratch.jslack.api.methods.request.users.UsersListRequest
import com.github.seratch.jslack.api.model.{Channel, User}
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

  val config = IO(_root_.pureconfig.loadConfigOrThrow[ApplicationConfig])
  val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")

  def toTimestampString(zonedDateTime: ZonedDateTime): String =
    zonedDateTime.toEpochSecond + "." + zonedDateTime.getNano

  implicit class RichSlackApiResponse[A <: SlackApiResponse](a: A) {

    def toEither: Either[RuntimeException, A] =
      Either.cond(a.isOk, a, new RuntimeException(a.getError))
  }

  def f(applicationConfig: ApplicationConfig): IO[Either[RuntimeException, List[Message]]] = IO {
    val slack: Slack = Slack.getInstance
    for {
      u <- UsersListRequest
        .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().usersList)
        .toEither.flatMap(
          _.getMembers.asScala
            .filter(_.getName === applicationConfig.slackUserName).toList.toNel
            .toRight(new RuntimeException("ユーザーが見つかりません"))
            .flatMap(a => Either.cond(a.length === 1, a.head, new RuntimeException("ユーザーが重複しています"))))
      c <- ChannelsListRequest
        .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().channelsList).toEither.flatMap(
          _.getChannels.asScala
            .find(_.getName === applicationConfig.slackChannelName).toRight(new RuntimeException("")))
      h <- g(slack, applicationConfig, c, ZonedDateTime.of(2019, 1, 31, 23, 59, 59, 0, zoneId), Nil).unsafeRunSync()
    } yield h
  }

  val slackCount: Int Refined Closed[W.`0`.T, W.`1000`.T] = 200

  def g(slack: Slack,
        applicationConfig: ApplicationConfig,
        c: Channel,
        until: ZonedDateTime,
        acc: List[Message]): IO[Either[RuntimeException, List[Message]]] =
    IO.fromEither(
        Try(
          ChannelsHistoryRequest
            .builder().token(applicationConfig.slackToken).channel(c.getId).count(slackCount.value).pipe { b =>
              acc.toNel.fold(b)(nel =>
                b.latest(toTimestampString(nel.map(_.ts).minimum))
                  .oldest(toTimestampString(nel.map(_.ts).minimum.minusDays(1))))
            }
            .build().pipe(slack.methods().channelsHistory)
            .toEither
            .map(_.getMessages.asScala.toList.map(m2m).sequence[Either[RuntimeException, ?], Message])
        ).toEither.joinRight).flatMap { h =>
        h.fold(
          a => IO.pure(a.asLeft),
          h => {
            logger.debug(h.length.show)
            h.toNel.fold[IO[Either[RuntimeException, List[Message]]]](IO.pure(acc.asRight))(
              h =>
                if (h.forall(a => acc.nonEmpty && acc.exists(_ === a)))
                  IO.pure(new RuntimeException("レスポンスボディの要素が重複しました").asLeft)
                else {
                  h.nonEmptyPartition(zdt => Either.cond(zdt.ts < until, zdt, zdt)).fold(
                      _ => IO.pure(acc.asRight),
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

  def m2m(a: com.github.seratch.jslack.api.model.Message): Either[RuntimeException, Message] =
    d2d(a.getTs).map(ts => slack.Message(a.getUser, ts, a.getText))

  def d2d(a: String): Either[RuntimeException, ZonedDateTime] =
    try {
      a.split('.').toList match {
        case second :: nano :: Nil =>
          ZonedDateTime
            .from(Instant.ofEpochSecond(second.toLong).atOffset(ZoneOffset.UTC)).withNano(nano.toInt * 1000).asRight
        case _ => new RuntimeException("tsのZonedDateTime変換が失敗しました").asLeft
      }
    } catch {
      case e: RuntimeException => e.asLeft
    }

  def run(args: List[String]): IO[ExitCode] =
    config.flatMap(f).flatMap(_.fold(IO.raiseError, _.traverse_(m => IO.pure(logger.info(m.ts.show))))).map { _ =>
//    IO {
//      println(ZonedDateTime.now(ZoneOffset.UTC))
//      println(ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(zoneId))
//      println(ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime)
      ExitCode.Success
    }

  def showLatestSummary(messages: NonEmptyList[Message], user: User): Either[RuntimeException, Summary] =
    for {
      myMessages <- messages
        .filter(_.user === user.getId).toNel
        .toRight(new RuntimeException(s"${user.getId} のメッセージが存在しません"))
      latestDate = myMessages
        .map(_.ts).maximum.withZoneSameInstant(zoneId).withHour(0).withMinute(0)
        .withSecond(0).withNano(0)
      adts <- myMessages
        .filter(_.ts > latestDate).toNel.toRight(new RuntimeException(s"$latestDate のメッセージが存在しません"))
        .flatMap(
          _.map(_.text.pipe(stringToAdt)).pipe(f_).leftMap(_.mkString_("", "\n", "").pipe(new RuntimeException(_))))
      summary <- adtsToSummary(adts.toNonEmptyList).toEither
        .leftMap(_.mkString_("", "\n", "").pipe(new RuntimeException(_)))
    } yield summary

  def stringToAdt(a: String): Either[String, Adt] = a match {
    case "open"       => Open.asRight
    case "afk" | "qk" => Afk.asRight
    case "back"       => Back.asRight
    case "close"      => Close.asRight
    case _            => s"${a}を${classOf[Adt].getName}に変換できません".asLeft
  }

  def f_(a: NonEmptyList[Either[String, Adt]]): Either[NonEmptyChain[String], NonEmptyChain[Adt]] =
    a.reduceLeftM(_.bimap(NonEmptyChain.one, NonEmptyChain.one))((b, m) => m.bimap(NonEmptyChain.one, b :+ _))

  def adtsToSummary(adts: NonEmptyList[Adt]): ValidatedNel[String, Summary] = ???

  case class Summary(open: ZonedDateTime, cloes: ZonedDateTime, restingDuration: Duration, workingDuration: Duration)

  sealed trait Adt
  object Open extends Adt
  object Afk extends Adt
  object Back extends Adt
  object Close extends Adt
}
