package com.github.shokohara.slack
import java.time.{DayOfWeek, LocalTime}

case class SummaryLocalTime(open: LocalTime,
                            close: LocalTime,
                            restingTime: LocalTime,
                            workingTime: LocalTime,
                            dayOfWeek: DayOfWeek,
                            holiday: Option[String])
