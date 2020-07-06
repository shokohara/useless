package com.github.shokohara.minsoku

import java.time.LocalDateTime

case class Row(
  address: String,
  localDateTime: LocalDateTime,
  dayOfWeek: String,
  typeOfLine: String,
  provider: String,
  typeOfHouse: String,
  internetConnectionMethod: String,
  typeOfConsole: String,
  osName: String,
  browser: String,
  ipV4ConnectionMethod: String,
  ipV4jitter: Double,
  ipV4ping: Double,
  ipV4down: Double,
  ipV4up: Double,
  ipV6ConnectionMethod: String,
  ipV6jitter: Double,
  ipV6ping: Double,
  ipV6down: Double,
  ipV6up: Double
)

object Row {

  def from(
    x: (
      String,
      LocalDateTime,
      String,
      String,
      String,
      String,
      String,
      String,
      String,
      String,
      Option[(String, Double, Double, Double, Double)],
      Option[(String, Double, Double, Double, Double)]
    )
  ): Row = {
    val (a, b, c, d, e, f, g, h, i, j, k, l) = x
    val default = ("", 0: Double, 0: Double, 0: Double, 0: Double)
    val (m, n, o, p, q) = k.getOrElse(default)
    val (r, s, t, u, v) = l.getOrElse(default)
    Row(a, b, c, d, e, f, g, h, i, j, m, n, o, p, q, r, s, t, u, v)
  }
}
