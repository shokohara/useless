package com.github.shokohara.minsoku

import java.time.LocalDateTime

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.chrome.{ChromeDriver, ChromeDriverService}
import org.openqa.selenium.{By, WebElement}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

object Main extends IOApp with LazyLogging {

  val chromeDriverService = Resource.make(IO(ChromeDriverService.createDefaultService()))(a => IO(a.stop()))

  val chromeDriver = chromeDriverService
    .flatMap(chromeDriverService => Resource.make(IO(new ChromeDriver(chromeDriverService)))(a => IO(a.quit())))

  def run(args: List[String]): IO[ExitCode] = {
    logger.warn(
      "出力されるCSVの後述の列とデータの対応がずれていた。発覚した時には既にこのプログラムを作成する当初の目的は達成したから修正をしていない。ipV4ConnectionMethod, ipV4jitter, ipV4ping, ipV4down, ipV4up, ipV6ConnectionMethod, ipV6jitter, ipV6ping, ipV6down, ipV6up"
    )
    val end = 300
    crawl(1 to end).use(_ => IO.unit).timeout(30.seconds * end).as(ExitCode.Success)
  }

  def communicationSpeedReports(wes: List[WebElement]): List[List[WebElement]] = {
    def isNotHeaderOfCommunicationSpeedReport(we: WebElement) =
      !(we.getAttribute("class") == "font-size-little-big" &&
        we.findElement(By.tagName("p")).getText.endsWith("通信速度レポート"))
    @scala.annotation.tailrec
    def f(wes: List[WebElement], result: List[List[WebElement]]): List[List[WebElement]] =
      wes match {
        case head :: last if isNotHeaderOfCommunicationSpeedReport(head) =>
          logger.debug(head.getAttribute("class"))
          f(last, result)
        case head :: last =>
          logger.debug(head.getAttribute("class"))
          val (elementsUntilNextHeader, remain) = last.span(isNotHeaderOfCommunicationSpeedReport)
          assert(elementsUntilNextHeader.forall(isNotHeaderOfCommunicationSpeedReport))
          f(remain, (head :: elementsUntilNextHeader) :: result)
        case Nil => result
      }
    f(wes, Nil)
  }

  def parse(
    string: String
  ): Either[
    MatchError,
    (
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
  ] = {
    val regex1 = "^回線タイプ: (.+)　プロバイダ: (.+)　住宅の種類: (.+)　ネット接続方法: (.+)$".r
    val regex2 = "^端末の種類: (.+)　OS名: (.+)　ブラウザ: (.+)$".r
    val regex3 = "^【IPv4接続】ジッター値: (.+)ms　Ping値: (.+)ms　ダウンロード速度: (.+)Mbps　アップロード速度: (.+)Mbps$".r
    val regex4 = "^【IPv6接続】ジッター値: (.+)ms　Ping値: (.+)ms　ダウンロード速度: (.+)Mbps　アップロード速度: (.+)Mbps$".r
    val regex5 = "^ジッター値: (.+)ms　Ping値: (.+)ms　ダウンロード速度: (.+)Mbps　アップロード速度: (.+)Mbps$".r
    Either
      .catchOnly[MatchError](string.linesIterator.toList match {
        case s"$address　${year}年${month}月${day}日(${dayOfWeek}) ${hour}時${minute}分" ::
            regex1(typeOfLine, provider, typeOfHouse, internetConnectionMethod) ::
            regex2(typeOfConsole, osName, browser) ::
            s"IPv4接続方式: $ipV4ConnectionMethod" ::
            s"IPv6接続方式: $ipV6ConnectionMethod" ::
            regex3(ipV4jitter, ipV4ping, ipV4down, ipV4up) ::
            regex4(ipV6jitter, ipV6ping, ipV6down, ipV6up) :: Nil =>
          (
            address,
            LocalDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt),
            dayOfWeek,
            typeOfLine,
            provider,
            typeOfHouse,
            internetConnectionMethod,
            typeOfConsole,
            osName,
            browser,
            (ipV4ConnectionMethod, ipV4jitter.toDouble, ipV4ping.toDouble, ipV4down.toDouble, ipV4up.toDouble).some,
            (ipV6ConnectionMethod, ipV6jitter.toDouble, ipV6ping.toDouble, ipV6down.toDouble, ipV6up.toDouble).some
          )
        case s"$address　${year}年${month}月${day}日(${weekOfDay}) ${hour}時${minute}分" ::
            regex1(typeOfLine, provider, typeOfHouse, internetConnectionMethod) ::
            regex2(typeOfConsole, osName, browser) ::
            s"IPv4接続方式: $ipV4ConnectionMethod" ::
            regex5(ipV4jitter, ipV4ping, ipV4down, ipV4up) :: Nil =>
          (
            address,
            LocalDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt),
            weekOfDay,
            typeOfLine,
            provider,
            typeOfHouse,
            internetConnectionMethod,
            typeOfConsole,
            osName,
            browser,
            (ipV4ConnectionMethod, ipV4jitter.toDouble, ipV4ping.toDouble, ipV4down.toDouble, ipV4up.toDouble).some,
            None
          )
        case s"$address　${year}年${month}月${day}日(${weekOfDay}) ${hour}時${minute}分" ::
            regex1(typeOfLine, provider, typeOfHouse, internetConnectionMethod) ::
            regex2(typeOfConsole, osName, browser) ::
            s"IPv6接続方式: $ipV6ConnectionMethod" ::
            regex5(ipV6jitter, ipV6ping, ipV6down, ipV6up) :: Nil =>
          (
            address,
            LocalDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt),
            weekOfDay,
            typeOfLine,
            provider,
            typeOfHouse,
            internetConnectionMethod,
            typeOfConsole,
            osName,
            browser,
            None,
            (ipV6ConnectionMethod, ipV6jitter.toDouble, ipV6ping.toDouble, ipV6down.toDouble, ipV6up.toDouble).some
          )
      })
  }

  def crawl(range: Range): Resource[IO, Unit] = {
    import io.chrisdavenport.cormorant._
    import io.chrisdavenport.cormorant.generic.auto._
    import io.chrisdavenport.cormorant.implicits._
    chromeDriver.map { driver =>
      logger.info(
        range
          .flatMap { i =>
            logger.info(s"progress: $i/${range.end}")
            val url = s"https://minsoku.net/speeds/optical/services/flets-next/prefectures/33?page=$i"
            driver.get(url)
            communicationSpeedReports(
              driver
                .findElementsByXPath("html/body/div[1]/div[3]/div/div[1]/*").listIterator().asScala.toList
            ).flatMap(wes =>
              parse(s"${wes(2).getText}\n${wes(3).getText}")
                .leftMap(e => logger.error(url, e.printStackTrace())).toOption
            ).map(Row.from)
          }.toList.writeComplete.print(Printer.default)
      )
    }
  }
}
