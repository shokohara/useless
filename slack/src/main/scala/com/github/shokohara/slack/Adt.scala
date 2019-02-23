package com.github.shokohara.slack

import java.time.ZonedDateTime

sealed abstract class Adt {
  def ts: ZonedDateTime
}

case class Open(ts: ZonedDateTime) extends Adt
case class Afk(ts: ZonedDateTime) extends Adt
case class Back(ts: ZonedDateTime) extends Adt
case class Close(ts: ZonedDateTime) extends Adt
