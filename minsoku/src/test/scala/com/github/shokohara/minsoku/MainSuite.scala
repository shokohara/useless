package com.github.shokohara.minsoku

import java.time.LocalDateTime

import cats.implicits._
import com.github.shokohara.minsoku.Main._
import munit.FunSuite

class MainSuite extends FunSuite {
  test("on page 1") {
    import cats.effect.{ContextShift, IO, Timer}
    import scala.concurrent.ExecutionContext
    import scala.concurrent.duration._
    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    val end = 1
    crawl(1 to end).use(_ => IO.unit).timeout(30.seconds * end).unsafeRunSync()
  }
  test("parse2") {
    assertEquals(
      parse("""岡山県岡山市東区　2020年07月05日(日) 17時20分
             |回線タイプ: 光回線　プロバイダ: BIGLOBE　住宅の種類: 戸建て住宅　ネット接続方法: 有線
             |端末の種類: デスクトップPC　OS名: windows　ブラウザ: Chrome
             |IPv4接続方式: PPPoE
             |IPv6接続方式: IPoE(IPv6オプション)
             |【IPv4接続】ジッター値: 10.04ms　Ping値: 22.0ms　ダウンロード速度: 463.13Mbps　アップロード速度: 255.9Mbps
             |【IPv6接続】ジッター値: 1.12ms　Ping値: 22.0ms　ダウンロード速度: 437.53Mbps　アップロード速度: 227.89Mbps""".stripMargin),
      (
        "岡山県岡山市東区",
        LocalDateTime.of(2020, 7, 5, 17, 20, 0),
        "日",
        "光回線",
        "BIGLOBE",
        "戸建て住宅",
        "有線",
        "デスクトップPC",
        "windows",
        "Chrome",
        ("PPPoE", 10.04, 22.0, 463.13, 255.9).some,
        ("IPoE(IPv6オプション)", 1.12, 22.0, 437.53, 227.89).some
      ).asRight
    )
  }
}
