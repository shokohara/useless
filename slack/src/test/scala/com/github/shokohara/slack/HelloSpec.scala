package com.github.shokohara.slack

import java.time._

import cats.data.NonEmptyList
import cats.data.Validated.Valid
import cats.implicits._
import com.github.shokohara.slack.Hello._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.chaining._

class HelloSpec extends FlatSpec with Matchers {
  "adtsToSummary" should "work" in {
    val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
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
      Open(openZdt) :: Afk(afk1Zdt) :: Back(backZdt) :: Afk(afk2Zdt) :: Close(closeZdt) :: Nil)

    Hello
      .adtsToSummary(adts).fold(_ => fail(),
                                _ shouldEqual Summary(
                                  openZdt,
                                  afk2Zdt,
                                  Duration.parse("PT22M24S"),
                                  Duration.parse("PT8H22M26S"),
                                  DayOfWeek.FRIDAY,
                                  None
                                ))
  }
  behavior of "stringToAdt"
  "RLGURERL7".pipe { userId =>
    it should "work with open" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T00:59:46.146700Z"), "open"))
    }
    it should "work with afk|qk" in {
      // このテストも通したい
//      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T07:39:51.139500Z"), "qk at 07:31")) shouldEqual
//        Afk(ZonedDateTime.parse("2019-02-19T07:31:00.000000Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:36:49.147600Z"), "afk")) shouldEqual
        Afk(ZonedDateTime.parse("2019-02-20T01:36:49.147600Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T05:58:53.155400Z"), "qk at "))
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T05:58:53.155400Z,qk at )をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T07:40:53.158Z"), "qk")) shouldEqual
        Afk(ZonedDateTime.parse("2019-02-20T07:40:53.158Z")).validNec.validNec
    }
    it should "work with back" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T09:31:53.159500Z"), "back")) shouldEqual
        Back(ZonedDateTime.parse("2019-02-20T09:31:53.159500Z")).validNec.validNec
      // このテストも通したい
//      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T06:29:22.157800Z"), "backed at 06:21")) shouldEqual
//        Back(ZonedDateTime.parse("2019-02-20T06:29:21.000000Z")).validNec.validNec
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:43:56.148400Z"), "bak"))
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T01:43:56.148400Z,bak)をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec

      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T07:57:53.139700Z"), "back"))
    }
    it should "work with close" in {
      // このテストも通したい
      //    stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-19T09:59:51.141600Z"), "closed at 09:43")) shouldEqual
      //      Close(ZonedDateTime.parse("2019-02-19T09:43:00.000000")).asRight.asRight
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T12:02:30.164Z"), "close")) shouldEqual
        Close(ZonedDateTime.parse("2019-02-20T12:02:30.164Z")).validNec.validNec
    }
    it should "work with other" in {
      stringToAdt(Message(userId, ZonedDateTime.parse("2019-02-20T01:44:03.148800Z"), "猫に投薬してました :cat2:"))
        .map(_.leftMap(_.map(_.getMessage))) shouldEqual
        s"Message($userId,2019-02-20T01:44:03.148800Z,猫に投薬してました :cat2:)をcom.github.shokohara.slack.Adtに変換できません".invalidNec.validNec
    }
  }
}
