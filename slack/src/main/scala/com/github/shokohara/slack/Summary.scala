package com.github.shokohara.slack

import java.time.{DayOfWeek, Duration, LocalTime, ZonedDateTime}
import cats.kernel.Eq
import com.github.shokohara.slack.Hello.zoneId

case class Summary(open: ZonedDateTime,
                   close: ZonedDateTime,
                   restingDuration: Duration,
                   workingDuration: Duration,
                   dayOfWeek: DayOfWeek,
                   holiday: Option[String]) {

  def toLocal = SummaryLocalTime(
    open = open.withZoneSameInstant(zoneId).toLocalTime,
    close = close.withZoneSameInstant(zoneId).toLocalTime,
    restingTime = LocalTime.of(0, 0).plus(restingDuration),
    workingTime = LocalTime.of(0, 0).plus(workingDuration),
    dayOfWeek = dayOfWeek,
    holiday = holiday
  )
}

object Summary {
  implicit val eq: Eq[Summary] = Eq.fromUniversalEquals[Summary]
}
