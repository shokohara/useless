package com.github.shokohara.slack

import java.time._

import cats.data.NonEmptyList
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
import jp.t2v.util.locale.Implicits._

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.chaining._

object Hello extends IOApp with LazyLogging {

  val config = IO(_root_.pureconfig.loadConfigOrThrow[ApplicationConfig])
//  val asiaTokyo: ZoneId = ZoneId.of("Asia/Tokyo")

  def toTimestampString(zonedDateTime: ZonedDateTime): String =
    zonedDateTime.toEpochSecond + "." + zonedDateTime.getNano

  implicit class RichSlackApiResponse[A <: SlackApiResponse](a: A) {

    def toEither: Either[RuntimeException, A] =
      Either.cond(a.isOk, a, new RuntimeException(a.getError))
  }

  def f(applicationConfig: ApplicationConfig,
        until: LocalDate,
        zoneId: ZoneId): IO[Either[RuntimeException, (List[Message], User)]] = IO {
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
      h <- g(slack, applicationConfig, c, until.atStartOfDay(zoneId), Nil).unsafeRunSync()
    } yield (h, u)
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

  def toSummary(applicationConfig: ApplicationConfig,
                until: LocalDate,
                zoneId: ZoneId): IO[Either[RuntimeException, Summary]] =
    f(applicationConfig, until, zoneId).flatMap(_.fold(IO.raiseError, {
      case (list, u) =>
        IO.fromEither(list.toNel.toRight(new RuntimeException("メッセージが存在しません")).map(latestSummary(_, u, zoneId)))
    }))

  def run(args: List[String]): IO[ExitCode] = {
    val asiaTokyo = ZoneId.of("Asia/Tokyo")
    config
      .flatMap(f(_, LocalDate.now().plusDays(1), asiaTokyo))
      .flatMap(_.fold(
        IO.raiseError, {
          case (list, u) =>
            IO.fromEither(
              list.toNel
                .toRight(new RuntimeException("メッセージが存在しません"))
                .flatMap(nel =>
                  latestSummary(nel, u, asiaTokyo).map(a => println(a.toLocal(asiaTokyo))).leftFlatMap { e =>
                    logger.error("", e)
                    latestWorkingDuration(nel, u, ZonedDateTime.now(asiaTokyo).some, asiaTokyo).map { d =>
                      println("Working: " + LocalTime.of(0, 0).plus(d._1))
                      println("Resting: " + LocalTime.of(0, 0).plus(d._2))
                    }
                }))
        }
      )).map(_ => ExitCode.Success)
  }

  def latestSummary(messages: NonEmptyList[Message], user: User, zoneId: ZoneId): Either[RuntimeException, Summary] =
    listLatestDateAdt(messages, user, zoneId: ZoneId).flatMap(adtsToSummary)

  def latestWorkingDuration(messages: NonEmptyList[Message],
                            user: User,
                            now: Option[ZonedDateTime],
                            zoneId: ZoneId): Either[RuntimeException, (Duration, Duration)] =
    listLatestDateAdt(messages, user, zoneId).flatMap(adtsToWorkingDuration(_, now))

  def listLatestDateAdt(messages: NonEmptyList[Message],
                        user: User,
                        zoneId: ZoneId): Either[RuntimeException, NonEmptyList[Adt]] =
    for {
      myMessages <- messages
        .filter(_.user === user.getId).toNel
        .toRight(new RuntimeException(s"${user.getId} のメッセージが存在しません"))
      latestDate = myMessages.map(_.ts).maximum.withZoneSameInstant(zoneId).toLocalDate
      summary <- myMessages
        .filter(_.ts.withZoneSameInstant(zoneId).toLocalDate === latestDate).toNel.toRight(
          new RuntimeException(s"$latestDate のメッセージが存在しません"))
        .flatMap(_.map(stringToAdt).sequence[Either[RuntimeException, ?], Either[RuntimeException, Adt]])
        .flatMap(_.toList.flatMap(_.toOption.toList).toNel.toRight(new RuntimeException("toNel")))
    } yield summary

  /**
    * SlackのメッセージをADTに変換します。<br>
    * - 始業はopen<br>
    * - 休憩開始はafkまたはqk<br>
    * - 休憩終了はback<br>
    * - 終業はclose<br>
    * 基本的に始業とともに"open"発言することでMessage.tsの時間が始業の時間を示す（例→open）。
    * 例外的に始業とともに"open"発言をし忘れた場合のためにopen時間を上書き可能にする文法がある（例→opened at 12:34）。
    * "at"文法は[opened|afk|qk|backed|closed]と組み合わせることができる。
    * "at "以降の時刻は24h表記かつ1桁時か1桁分の場合は先頭に0を加える形式（例→12:34 09:01）のみを許可する。
    * 許可されない形式だった場合は、雑談扱いにする。
    * これらの定義を満たさないメッセージは雑談扱いにする。
    * @param a Slackのメッセージ
    * @return `[計算結果を使った計算が続行されたくない場合, [警告として表示しつつ計算が続行されたい場合(雑談), 正常な結果]]`
    */
  def stringToAdt(a: Message): Either[RuntimeException, Either[RuntimeException, Adt]] =
    if (a.text === "open") Open(a.ts).asRight.asRight
    else if (a.text.startsWith("opened at ") || a.text.startsWith("opend at "))
      try {
        val timeText = a.text.reverse.takeWhile(_.isSpaceChar === false).reverse
        val localTime = LocalTime.parse(timeText)
        Open(a.ts.withHour(localTime.getHour).withMinute(localTime.getMinute)).asRight.asRight
      } catch { case e: RuntimeException => e.asLeft } else if (a.text === "afk" || a.text === "qk")
      Afk(a.ts).asRight.asRight
    else if (a.text === "back") Back(a.ts).asRight.asRight
    else if (a.text === "close") Close(a.ts).asRight.asRight
    else
      new RuntimeException(s"${a}を${classOf[Adt].getName}に変換できません").asLeft.asRight: Either[
        RuntimeException,
        Either[RuntimeException, Adt]]

  def adtsToWorkingDuration(adts: NonEmptyList[Adt],
                            now: Option[ZonedDateTime]): Either[RuntimeException, (Duration, Duration)] =
    validateAdts(adts).flatMap { opens =>
      logger.debug(adts.filter(a => isAfk(a) || isBack(a)).sortBy(_.ts).toString())
      adts
        .filter(a => isAfk(a) || isBack(a)).sortBy(_.ts).foldLeftM[Either[RuntimeException, ?],
                                                                   (Duration, Option[Adt])]((Duration.ZERO, None)) {
          case ((d, opt), a) =>
            logger.debug((d, opt, a).toString())
            (opt, a) match {
              case (None, a @ Afk(_))         => (d, a.some).asRight
              case (Some(Afk(_)), a @ Afk(_)) => (d, a.some).asRight
              case (Some(afk @ Afk(_)), back @ Back(_)) =>
                (d.plus(Duration.between(afk.ts, back.ts)), back.some).asRight
              case (Some(Back(_)), afk @ Afk(_)) => (d, afk.some).asRight
              // Back Backで例外
              case _ => new RuntimeException("").asLeft
            }
        }.map(_._1).flatMap { resting =>
          logger.debug(s"休憩時間: $resting")
          val open = adts.filter(isOpen).head.ts
          adts.sortBy(_.ts).last match {
            case Back(_)   => (resting, Duration.between(now.get, open).abs().minus(resting)).asRight
            case Afk(ts)   => (resting, Duration.between(ts, open).abs().minus(resting)).asRight
            case Close(ts) => (resting, Duration.between(ts, open).abs().minus(resting)).asRight
            case _         => new RuntimeException("").asLeft
          }
        }
    }

  def adtsToSummary(adts: NonEmptyList[Adt]): Either[RuntimeException, Summary] = {
    logger.debug(adts.toString)
    validateAdts(adts).flatMap { opens =>
      adtsToWorkingDuration(adts, None).map({
        case (r, w) =>
          Summary(
            open = adts.filter(isOpen).head.ts,
            close = if (isAfk(adts.init.last)) adts.init.last.ts else adts.filter(isClose).head.ts,
            restingDuration = r,
            workingDuration = w,
            dayOfWeek = opens.head.ts.toLocalDate.getDayOfWeek,
            holiday = opens.head.ts.toLocalDate.holidayName
          )
      })
    }: Either[RuntimeException, Summary]
  }

  def validateAdts(adts: NonEmptyList[Adt]): Either[RuntimeException, NonEmptyList[Adt]] = {
    logger.debug(adts.toString)
    adts.filter(isOpen).toNel.toRight(new RuntimeException("Openが0です")).flatMap { opens =>
      if (opens.length > 1)
        new RuntimeException("Openが複数存在します").asLeft
      else if (adts.count(isClose).isEmpty)
        new RuntimeException("Closeが0です").asLeft
      else if (adts.count(isClose) > 1)
        new RuntimeException("Closeが複数存在します").asLeft
      else if (adts.count(isAfk) === adts.count(isBack) || adts.count(isAfk) === adts.count(isBack) + 1)
        opens.asRight
      else new RuntimeException(s"Afkの回数とBackの回数が不正です Afk: ${adts.count(isAfk)} Back: ${adts.count(isBack)}").asLeft
    }
  }

  val isOpen: Adt => Boolean = (_: Adt) match {
    case Open(_) => true
    case _       => false
  }

  val isAfk: Adt => Boolean = (_: Adt) match {
    case Afk(_) => true
    case _      => false
  }

  val isBack: Adt => Boolean = (_: Adt) match {
    case Back(_) => true
    case _       => false
  }

  val isClose: Adt => Boolean = (_: Adt) match {
    case Close(_) => true
    case _        => false
  }
}
