package com.github.shokohara.slack

import java.time.ZonedDateTime

sealed abstract class Adt {
  def ts: ZonedDateTime
}

final case class Open(ts: ZonedDateTime) extends Adt
final case class Afk(ts: ZonedDateTime) extends Adt
final case class Back(ts: ZonedDateTime) extends Adt
final case class Close(ts: ZonedDateTime) extends Adt
