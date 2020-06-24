package com.github.shokohara.slack

import java.time._

import cats.data.{NonEmptyList, ValidatedNec}
import cats.data.Validated.Valid
import cats.implicits._
import com.github.shokohara.slack.Hello._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.chaining._

class HelloSpec extends FlatSpec with Matchers {
  val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
  "adtsToSummary" should "work" in {
    val localDate = LocalDate.of(2019, 2, 15)
    val openTime = LocalTime.of(11, 31, 52)
    val afk1Time = LocalTime.of(14, 47, 33)
    val backTime = LocalTime.of(15, 9, 57)
    val afk2Time = LocalTime.of(17, 49, 58)
    val closeTime = LocalTime.of(20, 16, 42)
    val openZdt = ZonedDateTime.of(localDate, openTime, zoneId)
    val afk1Zdt = ZonedDateTime.of(localDate, afk1Time, zoneId)
    val backZdt = ZonedDateTime.of(localDate, backTime, zoneId)
    val afk2Zdt = ZonedDateTime.of(localDate, afk2Time, zoneId)
    val closeZdt = ZonedDateTime.of(localDate, closeTime, zoneId)
    val adts: NonEmptyList[Adt] = NonEmptyList.fromListUnsafe(
      Open(openZdt) :: Afk(afk1Zdt) :: Back(backZdt) :: Afk(afk2Zdt) :: Close(closeZdt) :: Nil
    )

    Hello
      .adtsToSummary(adts).fold(
        _ => fail(),
        _ shouldEqual Summary(
          openZdt,
          afk2Zdt,
          Duration.parse("PT22M24S"),
          Duration.parse("PT8H22M26S"),
          DayOfWeek.FRIDAY,
          None
        )
      )
  }
  behavior of "stringToAdt"
  "RLGURERL7".pipe { userId =>
    it should "work with open" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T00:59:46.146700Z"), "open"), zoneId)
    }
    it should "work with afk|qk" in {
      // このテストも通したい
//      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T07:39:51.139500Z"), "qk at 07:31")) shouldEqual
//        Afk(ZonedDateTime.parse("2019-02-19T07:31:00.000000Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:36:49.147600Z"), "afk"), zoneId) shouldEqual
        Afk(ZonedDateTime.parse("2019-02-20T01:36:49.147600Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T05:58:53.155400Z"), "qk at "), zoneId)
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T05:58:53.155400Z,qk at )をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T07:40:53.158Z"), "qk"), zoneId) shouldEqual
        Afk(ZonedDateTime.parse("2019-02-20T07:40:53.158Z")).validNec.validNec
    }
    it should "work with back" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T09:31:53.159500Z"), "back"), zoneId) shouldEqual
        Back(ZonedDateTime.parse("2019-02-20T09:31:53.159500Z")).validNec.validNec
      // このテストも通したい
//      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T06:29:22.157800Z"), "backed at 06:21")) shouldEqual
//        Back(ZonedDateTime.parse("2019-02-20T06:29:21.000000Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:43:56.148400Z"), "bak"), zoneId)
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T01:43:56.148400Z,bak)をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec

      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T07:57:53.139700Z"), "back"), zoneId)
    }
    it should "work with close" in {
      // このテストも通したい
      //    stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T09:59:51.141600Z"), "closed at 09:43")) shouldEqual
      //      Close(ZonedDateTime.parse("2019-02-19T09:43:00.000000")).asRight.asRight
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T12:02:30.164Z"), "close"), zoneId) shouldEqual
        Close(ZonedDateTime.parse("2019-02-20T12:02:30.164Z")).validNec.validNec
    }
    it should "work with other" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:44:03.148800Z"), "猫に投薬してました :cat2:"), zoneId)
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T01:44:03.148800Z,猫に投薬してました :cat2:)をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec
    }
  }
  it should "work" in {
    adtsToSummary(
      NonEmptyList.of(
        Close(ZonedDateTime.parse("2019-03-21T14:53:03.136600Z")),
        Back(ZonedDateTime.parse("2019-03-21T08:18:45.131800Z")),
        Afk(ZonedDateTime.parse("2019-03-21T05:46:00.099600Z")),
        Open(ZonedDateTime.parse("2019-03-21T02:26:06.091800Z"))
      )
    ) shouldEqual Summary(
      ZonedDateTime.parse("2019-03-21T02:26:06.091800Z"),
      ZonedDateTime.parse("2019-03-21T14:53:03.136600Z"),
      Duration.parse("PT2H32M45.0322S"),
      Duration.parse("PT9H54M12.0126S"),
      DayOfWeek.THURSDAY,
      "春分の日".some
    ).valid
  }
  it should """work with "open", "close PRの対応しました" """ in {
    val a: List[ValidatedNec[RuntimeException, ValidatedNec[RuntimeException, Adt]]] =
      (Message("user", ZonedDateTime.parse("2019-04-25T21:38:06.091800Z"), "open") ::
        Message("user", ZonedDateTime.parse("2019-04-25T22:49:06.091800Z"), "close PRの対応しました") :: Nil)
        .map(stringToAdt(_, zoneId))
    val b: NonEmptyList[Adt] = a.map(_.fold(_ => ???, _.fold(_ => ???, identity))).toNel.get
    adtsToSummary(b) shouldEqual Valid(
      Summary(
        ZonedDateTime.parse("2019-04-25T21:38:06.091800Z"),
        ZonedDateTime.parse("2019-04-25T22:49:06.091800Z"),
        Duration.parse("PT0S"),
        Duration.parse("PT1H11M"),
        DayOfWeek.THURSDAY,
        None
      )
    )
  }
  it should """open close""" in {
    val a: List[ValidatedNec[RuntimeException, ValidatedNec[RuntimeException, Adt]]] =
      (Message("user", ZonedDateTime.parse("2019-05-03T10:39:07.091800Z"), "open") ::
        Message("user", ZonedDateTime.parse("2019-05-03T23:23:27.091800Z"), "close") :: Nil).map(stringToAdt(_, zoneId))
    val b: NonEmptyList[Adt] = a.map(_.fold(_ => ???, _.fold(_ => ???, identity))).toNel.get
    adtsToSummary(b) shouldEqual Valid(
      Summary(
        ZonedDateTime.parse("2019-05-03T10:39:07.091800Z"),
        ZonedDateTime.parse("2019-05-03T23:23:27.091800Z"),
        Duration.parse("PT0S"),
        Duration.parse("PT12H44M20S"),
        DayOfWeek.FRIDAY,
        Some("憲法記念日")
      )
    )
  }
}
