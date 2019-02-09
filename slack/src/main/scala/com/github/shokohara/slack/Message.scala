package com.github.shokohara.slack

import java.time.ZonedDateTime

case class Message(user: String, ts: ZonedDateTime, text: String)
