package controllers

import java.time.{DayOfWeek, LocalDate, ZoneId}

import cats.effect.IO
import com.github.shokohara.slack.Hello.Summary
import com.github.shokohara.slack.{ApplicationConfig, Hello}
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto._
import io.circe.java8.time._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.chaining._

class SlackController(cc: ControllerComponents)(implicit val ec: ExecutionContext)
  extends AbstractController(cc) with Circe {
  import SlackController._

  implicit val dayOfWeekEncoder: Encoder[DayOfWeek] = Encoder.instance(_.name().pipe(Json.fromString))
  implicit val summaryEncoder: Encoder[Summary] = deriveEncoder

  def index: Action[Request] = Action.async(circe.json[Request]) { request =>
    ApplicationConfig(request.body.token, request.body.channelName, request.body.userName)
      .pipe(Hello.toSummary(_, request.body.localDate.atStartOfDay(zoneId))).pipe(
        _.flatMap(_.fold(IO.raiseError, IO.pure)).unsafeToFuture().map(_.asJson).map(Ok(_)))
  }
}

object SlackController {
  val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
  final case class Request(token: NonEmptyString,
                           channelName: NonEmptyString,
                           userName: NonEmptyString,
                           localDate: LocalDate)

  object Request {
    implicit val decoder: Decoder[Request] = deriveDecoder
  }
}
