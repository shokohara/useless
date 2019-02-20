package com.github.shokohara.slack

import java.time._

import cats.data.NonEmptyList
import cats.implicits._
import com.github.shokohara.slack.Hello._
import org.scalatest.{FlatSpec, Matchers}

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
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

//    Hello.adtsToSummary(adts).right.get shouldEqual Summary(
//      openZdt,
//      afk2Zdt,
//      Duration.ZERO,
//      Duration.ZERO,
//      DayOfWeek.FRIDAY,
//      None
//    )
  }
}
