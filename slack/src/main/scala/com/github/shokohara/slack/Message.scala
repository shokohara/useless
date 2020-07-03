package com.github.shokohara.slack

import java.time.ZonedDateTime

import cats.Show

final case class Message(user: String, ts: ZonedDateTime, text: String)

object Message {
  implicit val show: Show[Message] = Show.fromToString
}
