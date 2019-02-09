package com.github.shokohara.slack

import akka.actor.ActorSystem
import cats.effect._
import cats.implicits._
import org.joda.time.DateTime
import play.api.libs.json._
import slack.api.{BlockingSlackApiClient, HistoryChunk}

import scala.util.Try

case class ApplicationConfig(token: String, channelName: String, userName: String)
object ApplicationConfig{

}

case class Message(user: String, ts: DateTime, text: String)

object Message {

  import play.api.libs.json.Json

  implicit object DateTimeReads extends Reads[DateTime] {
    def reads(json: JsValue) = json match {
      case JsString(value) => Try {
        new DateTime(value.split('.').head.toLong * 1000) // "The bit before the . is a unix timestamp, the bit after is a sequence to guarantee uniqueness."
      }.map(JsSuccess[DateTime](_)).getOrElse(throw new Exception(s"Could not parse a date-time out of $value"))
      case _ => JsError()
    }
  }

  implicit val reads = Json.reads[Message]
}

object Hello extends IOApp {
  import cats.syntax.apply._

  val r: Resource[IO, ActorSystem] = Resource.make(IO(ActorSystem("slack")))(a => IO(a.terminate()))

  import pureconfig.generic.auto._
  val config = IO(pureconfig.loadConfigOrThrow[ApplicationConfig])

  def toTimestampString(dateTime: DateTime): String = dateTime.getMillis + ".0000"

  def a(latest: DateTime)(implicit a: ActorSystem): IO[Option[List[Message]]] = config.map { config =>
    val client = BlockingSlackApiClient(config.token)
    for {
      u <- client.listUsers().find(_.name === "shopublic")
      c <- client.listChannels().find(_.name === "random")
    } yield {
      val f = (_: HistoryChunk).messages.toList.flatMap(_.asOpt[Message].toList).filter(_.user === u.id)
      val h = f(client.getChannelHistory(c.id, oldest = toTimestampString(latest).some))
//      val h = f(client.getChannelHistory(c.id, oldest = toTimestampString(h.sortBy(_.ts.getMillis).head.ts).some))
//      h.foreach(println)
//      println(h.sortBy(_.ts.getMillis).head.ts)
//      h.foreach(println)
      //      h.foreach(println)
      h
    }
  }

  def run(args: List[String]): IO[ExitCode] =
    r.use { implicit as =>
      a(DateTime.now()).flatMap().map(_ => ExitCode.Success)
    }
}

trait Greeting {
  lazy val greeting: String = "hello"
}
