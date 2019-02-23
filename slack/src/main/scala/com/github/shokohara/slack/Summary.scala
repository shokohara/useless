package com.github.shokohara.slack

import java.time._

import cats.kernel.Eq

case class Summary(open: ZonedDateTime,
                   close: ZonedDateTime,
                   restingDuration: Duration,
                   workingDuration: Duration,
                   dayOfWeek: DayOfWeek,
                   holiday: Option[String]) {

  def toLocal(zoneId: ZoneId) = SummaryLocalTime(
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
