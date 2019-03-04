package controllers

import java.time.{DayOfWeek, LocalDate, ZoneId}

import cats.data.EitherT
import cats.effect.{Effect, IO}
import cats.implicits._
import com.github.shokohara.slack.{ApplicationConfig, Hello, SummaryLocalTime}
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.generic.semiauto._
import io.circe.java8.time._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import play.api.libs.circe.Circe
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.util.chaining._

class SlackController(cc: ControllerComponents)(implicit val ec: ExecutionContext)
  extends AbstractController(cc) with Circe {
  import SlackController._

  implicit val dayOfWeekEncoder: Encoder[DayOfWeek] = Encoder.instance(_.name().pipe(Json.fromString))
  implicit val summaryEncoder: Encoder[SummaryLocalTime] = deriveEncoder

  def toTsv(local: SummaryLocalTime): String =
    s"${local.open}\t${local.close}\t${local.restingTime}\t${local.workingTime}"

  def index: Action[Request] = Action.asyncF(circe.json[Request]) { request =>
    val c = ApplicationConfig(request.body.token, request.body.channelName, request.body.userName)
    Hello
      .toSummary(c, request.body.localDate.plusDays(1), request.body.zoneId).map(_.map(_.toLocal(request.body.zoneId)))
      .flatMap(_.fold(IO.raiseError, IO.pure))
      .map { sl =>
        request.acceptedTypes.toList
          .find(mediaRange => mediaRange.mediaType === "text" && mediaRange.mediaSubType === "tab-separated-values")
          .fold(Ok(sl.asJson))(_ => Ok(toTsv(sl)))
      }
  }
}

object SlackController {
  final case class Request(token: NonEmptyString,
                           channelName: NonEmptyString,
                           userName: NonEmptyString,
                           localDate: LocalDate,
                           zoneId: ZoneId)

  object Request {
    implicit val decoder: Decoder[Request] = deriveDecoder
  }

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  implicit class ActionBuilderOps[+R[_], B](ab: ActionBuilder[R, B]) {
    import cats.effect.implicits._
    import cats.implicits._

    def asyncF[F[_]: Effect](cb: R[B] => F[Result]): Action[B] = ab.async { c =>
      cb(c).toIO.unsafeToFuture()
    }

    def asyncF[F[_]: Effect, A](
      bp: BodyParser[A]
    )(cb: R[A] => F[Result]): Action[A] =
      ab.async[A](bp) { c =>
        cb(c).toIO.unsafeToFuture()
      }

    def asyncEitherT[F[_]: Effect](
      cb: R[B] => EitherT[F, Result, Result]
    ): Action[B] = ab.async { c =>
      cb(c).value.map(_.merge).toIO.unsafeToFuture()
    }

    def asyncEitherT[F[_]: Effect, A](
      bp: BodyParser[A]
    )(cb: R[A] => EitherT[F, Result, Result]): Action[A] =
      ab.async[A](bp) { c =>
        cb(c).value.map(_.merge).toIO.unsafeToFuture()
      }
  }
}
