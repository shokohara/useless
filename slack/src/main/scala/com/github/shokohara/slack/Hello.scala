package com.github.shokohara.slack

import java.time._

import cats.data._
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
    s"""${zonedDateTime.toEpochSecond}.${zonedDateTime.getNano}"""

  implicit class RichSlackApiResponse[A <: SlackApiResponse](a: A) {

    def toEither: Either[RuntimeException, A] =
      Either.cond(a.isOk, a, new RuntimeException(a.getError))

    def toValidateNec: ValidatedNec[RuntimeException, A] =
      Validated.condNec(a.isOk, a, new RuntimeException(a.getError))
  }

  def f(applicationConfig: ApplicationConfig,
        until: LocalDate,
        zoneId: ZoneId): IO[ValidatedNec[RuntimeException, (List[Message], User)]] = IO {
    val slack: Slack = Slack.getInstance
    UsersListRequest
      .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().usersList)
      .validNec.andThen(
        _.getMembers.asScala
          .filter(_.getName === applicationConfig.slackUserName).toList.toNel
          .toRight(new RuntimeException("ユーザーが見つかりません")).toValidatedNec
          .andThen(a => Validated.condNec(a.length === 1, a.head, new RuntimeException("ユーザーが重複しています"))))
      .andThen { u =>
        ChannelsListRequest
          .builder().token(applicationConfig.slackToken).build().pipe(slack.methods().channelsList).validNec.andThen(
            _.getChannels.asScala
              .find(a => applicationConfig.slackChannelNames.exists(a.getName === _)).toRight(new RuntimeException(""))
              .toValidatedNec)
          .andThen { c =>
            g(slack, applicationConfig, c, until.atStartOfDay(zoneId), Nil).unsafeRunSync().andThen { h =>
              (h, u).validNec
            }
          }
      }
  }

  val slackCount: Int Refined Closed[W.`0`.T, W.`1000`.T] = 200

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def g(slack: Slack,
        applicationConfig: ApplicationConfig,
        c: Channel,
        until: ZonedDateTime,
        acc: List[Message]): IO[ValidatedNec[RuntimeException, List[Message]]] =
    IO(
      Try(
        ChannelsHistoryRequest
          .builder().token(applicationConfig.slackToken).channel(c.getId).count(slackCount.value).pipe { b =>
            acc.toNel.fold(b)(nel =>
              b.latest(toTimestampString(nel.map(_.ts).minimum))
                .oldest(toTimestampString(nel.map(_.ts).minimum.minusDays(1))))
          }
          .build().pipe(slack.methods().channelsHistory)
          .toEither
          .map(_.getMessages.asScala.toList.traverse[ValidatedNec[RuntimeException, ?], Message](m2m))
          .toValidatedNec
      ).toEither.toValidatedNec.combineAll.combineAll).map(
      _.andThen { listMessage =>
        logger.debug(listMessage.length.show)
        listMessage.toNel.fold[ValidatedNec[RuntimeException, List[Message]]](acc.validNec)(
          h =>
            if (h.forall(a => acc.nonEmpty && acc.exists(_ === a)))
              new RuntimeException("レスポンスボディの要素が重複しました").invalidNec
            else {
              h.nonEmptyPartition(zdt => Either.cond(zdt.ts < until, zdt, zdt)).fold(
                  _ => acc.validNec,
                  n => {
                    logger.debug((n ++ acc).map(_.ts).maximum.show)
                    logger.debug((n ++ acc).map(_.ts).minimum.show)
                    logger.debug("g")
                    g(slack, applicationConfig, c, until, acc ++ n.toList).unsafeRunSync()
                  },
                  (left, right) => {
                    logger.debug(left.map(_.ts).map(_.show).mkString_("", "\n", ""))
                    logger.debug(right.map(_.ts).map(_.show).mkString_("", "\n", ""))
                    (acc ++ right.toList).validNec
                  }
                )
          }
        )
      }
    )

  def m2m(a: com.github.seratch.jslack.api.model.Message): ValidatedNec[RuntimeException, Message] =
    d2d(a.getTs).map(ts => slack.Message(a.getUser, ts, a.getText))

  def d2d(a: String): ValidatedNec[RuntimeException, ZonedDateTime] =
    try {
      a.split('.').toList match {
        case second :: nano :: Nil =>
          ZonedDateTime
            .from(Instant.ofEpochSecond(second.toLong).atOffset(ZoneOffset.UTC)).withNano(nano.toInt * 1000).validNec
        case _ => new RuntimeException("tsのZonedDateTime変換が失敗しました").invalidNec
      }
    } catch {
      case e: RuntimeException => e.invalidNec
    }

  def toSummary(applicationConfig: ApplicationConfig,
                until: LocalDate,
                zoneId: ZoneId): IO[ValidatedNec[RuntimeException, Summary]] =
    f(applicationConfig, until, zoneId).map(_.andThen {
      case (list, u) =>
        list.toNel.toValidNec(new RuntimeException("メッセージが存在しません")).andThen(latestSummary(_, u, zoneId))
    })

  def run(args: List[String]): IO[ExitCode] = {
    val asiaTokyo = ZoneId.of("Asia/Tokyo")
    for {
      c <- config
      d <- IO(f(c, LocalDate.now().plusDays(1), asiaTokyo).map(_.andThen {
        case (list, u) =>
          list.toNel
            .toRight(new RuntimeException("メッセージが存在しません")).toValidatedNec
            .andThen(nel =>
              latestSummary(nel, u, asiaTokyo).andThen { _ =>
                latestWorkingDuration(nel, u, ZonedDateTime.now(asiaTokyo).some, asiaTokyo)
            })
      }))
      result <- d.map(
        _.fold(
          exceptions => {
            exceptions.toList.foreach(logger.warn(sourcecode.Enclosing(), _: Throwable))
            ExitCode.Error
          },
          y => {
            println(s"""Resting:${LocalTime.of(0, 0).plus(y._1)}""")
            println(s"""Working:${LocalTime.of(0, 0).plus(y._2)}""")
            ExitCode.Success
          }
        ))
    } yield result
  }

  def latestSummary(messages: NonEmptyList[Message],
                    user: User,
                    zoneId: ZoneId): ValidatedNec[RuntimeException, Summary] =
    listLatestDateAdt(messages, user, zoneId: ZoneId).andThen(adtsToSummary)

  def latestWorkingDuration(messages: NonEmptyList[Message],
                            user: User,
                            now: Option[ZonedDateTime],
                            zoneId: ZoneId): ValidatedNec[RuntimeException, (Duration, Duration)] =
    listLatestDateAdt(messages, user, zoneId).andThen(adtsToWorkingDuration(_, now))

  def listLatestDateAdt(messages: NonEmptyList[Message],
                        user: User,
                        zoneId: ZoneId): ValidatedNec[RuntimeException, NonEmptyList[Adt]] =
    messages
      .filter(_.user === user.getId).toNel
      .toRight(new RuntimeException(s"${user.getId} のメッセージが存在しません")).toValidatedNec
      .andThen { myMessages =>
        val latestDate = myMessages.map(_.ts).maximum.withZoneSameInstant(zoneId).toLocalDate
        myMessages
          .filter(_.ts.withZoneSameInstant(zoneId).toLocalDate === latestDate).toNel
          .toRight(new RuntimeException(s"$latestDate のメッセージが存在しません")).toValidatedNec
          .andThen(_.traverse[ValidatedNec[RuntimeException, ?], ValidatedNec[RuntimeException, Adt]](stringToAdt))
          .andThen(_.toList.flatMap(_.toOption.toList).toNel.toRight(new RuntimeException("toNel")).toValidatedNec)
      }

  /**
    * SlackのメッセージをADTに変換します。<br>
    * - 始業はopen<br>
    * - 休憩開始はafkまたはqk<br>
    * - 休憩終了はback<br>
    * - 終業はclose<br>
    * 基本的に始業とともに"open"発言することでMessage.tsの時間が始業の時間を示す（例→open）。
    * 例外的に始業とともに"open"発言をし忘れた場合のためにopen時間を上書き可能にする文法がある（例→opened at 12:34）。
    * "at"文法は[opened|afk|qk|backed|closed]と組み合わせることができる。
    * "at "以降の時刻は発言時刻より昔の時刻かつ24h表記かつ1桁時か1桁分の場合は先頭に0を加える形式（例→12:34 09:01）のみを許可する。
    * 許可されない形式だった場合は、雑談扱いにする。
    * これらの定義を満たさないメッセージは雑談扱いにする。
    * @param a Slackのメッセージ
    * @return `[計算結果を使った計算が続行されたくない場合, [警告として表示しつつ計算が続行されたい場合(雑談), 正常な結果]]`
    */
  def stringToAdt(a: Message): ValidatedNec[RuntimeException, ValidatedNec[RuntimeException, Adt]] =
    if (a.text === "open" || a.text === "開店") Open(a.ts).validNec.validNec
    else if (a.text.startsWith("opened at ") || a.text.startsWith("opend at "))
      try {
        val timeText = a.text.reverse.takeWhile(_.isSpaceChar === false).reverse
        val localTime = LocalTime.parse(timeText)
        Open(a.ts.withHour(localTime.getHour).withMinute(localTime.getMinute)).validNec.validNec
      } catch { case e: RuntimeException => e.invalidNec } else if (a.text === "afk" || a.text === "qk")
      Afk(a.ts).validNec.validNec
    else if (a.text === "back") Back(a.ts).validNec.validNec
    else if (a.text === "close" || a.text === "閉店") Close(a.ts).validNec.validNec
    else
      new RuntimeException(s"${a}を${classOf[Adt].getName}に変換できません").invalidNec.validNec: ValidatedNec[
        RuntimeException,
        ValidatedNec[RuntimeException, Adt]]

  def adtsToWorkingDuration(adts: NonEmptyList[Adt],
                            now: Option[ZonedDateTime]): ValidatedNec[RuntimeException, (Duration, Duration)] = {
    logger.debug(adts.filter(a => isAfk(a) || isBack(a)).sortBy(_.ts).toString())
    adts
      .filter(a => isAfk(a) || isBack(a)).sortBy(_.ts).foldLeftM[Either[RuntimeException, ?], (Duration, Option[Adt])](
        (Duration.ZERO, None)) {
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
      }.map(_._1).toValidatedNec.andThen { resting =>
        logger.debug(s"休憩時間: $resting")
        adts.filter(isOpen).headOption.toRight(new RuntimeException).toValidatedNec.andThen { open =>
          adts.sortBy(_.ts).last match {
            case Back(_) =>
              now
                .toRight(new RuntimeException).toValidatedNec.andThen(zone =>
                  (resting, Duration.between(zone, open.ts).abs().minus(resting)).validNec)
            case Afk(ts)   => (resting, Duration.between(ts, open.ts).abs().minus(resting)).validNec
            case Close(ts) => (resting, Duration.between(ts, open.ts).abs().minus(resting)).validNec
            case _         => new RuntimeException("").invalidNec
          }
        }
      }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def adtsToSummary(adts: NonEmptyList[Adt]): ValidatedNec[RuntimeException, Summary] = {
    logger.debug(adts.toString)
    adts.init.lastOption
      .toValidNec(new RuntimeException("")).andThen(
        a =>
          (validateAdts(adts),
           adtsToWorkingDuration(adts, None),
           adts.filter(isOpen).headOption.toRight(new RuntimeException("")).toValidatedNec,
           (if (isAfk(a)) adts.init.lastOption else adts.filter(isClose).headOption)
             .toRight(new RuntimeException)
             .toValidatedNec).mapN {
            case (opens, b, open, close) =>
              Summary(
                open.ts,
                close.ts,
                restingDuration = b._1,
                workingDuration = b._2,
                dayOfWeek = opens.head.ts.toLocalDate.getDayOfWeek,
                holiday = opens.head.ts.toLocalDate.holidayName
              )
        })
  }

  def validateAdts(adts: NonEmptyList[Adt]): ValidatedNec[RuntimeException, NonEmptyList[Adt]] = {
    logger.debug(adts.toString)
    adts.filter(isOpen).toNel.toRight(new RuntimeException("Openが0です")).toValidatedNec.andThen { opens =>
      if (opens.length > 1)
        new RuntimeException("Openが複数存在します").invalidNec
      else if (adts.count(isClose).isEmpty)
        new RuntimeException("Closeが0です").invalidNec
      else if (adts.count(isClose) > 1)
        new RuntimeException("Closeが複数存在します").invalidNec
      else if (adts.count(isAfk) === adts.count(isBack) || adts.count(isAfk) === adts.count(isBack) + 1)
        opens.validNec
      else
        new RuntimeException(s"Afkの回数とBackの回数が不正です Afk: ${adts.count(isAfk)} Back: ${adts.count(isBack)}").invalidNec
    }
  }

  def validateMessage(message: List[Message], bool: Boolean): ValidatedNec[RuntimeException, List[Message]] = {
    def f(a: List[Message]): Boolean =
      a match {
        case head :: secondHead :: tail =>
          if (head.ts < secondHead.ts)
            f(secondHead :: tail)
          else false
        case _ :: Nil => true
        case Nil      => true
      }
    if (f(message) === true) message.validNec
    else new RuntimeException("リストが不正です").invalidNec
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
