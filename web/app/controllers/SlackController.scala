package controllers

import cats.effect.IO
import com.github.shokohara.slack.{ApplicationConfig, Hello}
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.refined._
import io.circe.syntax._
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.chaining._

class SlackController(cc: ControllerComponents)(implicit val ec: ExecutionContext)
  extends AbstractController(cc) with Circe {
  import SlackController._

  def index: Action[Request] = Action.async(circe.json[Request]) { request =>
    import io.circe.generic.auto._
    ApplicationConfig(request.body.token, request.body.channelName, request.body.userName)
      .pipe(Hello.f).pipe(_.flatMap(_.fold(IO.raiseError, IO.pure)).unsafeToFuture().map(_.asJson).map(Ok(_)))
  }

  def index2(token: NonEmptyString, channelName: NonEmptyString, userName: NonEmptyString): Action[AnyContent] =
    Action.async {
      import io.circe.generic.auto._
      ApplicationConfig(token, channelName, userName)
        .pipe(Hello.f).pipe(_.flatMap(_.fold(IO.raiseError, IO.pure)).unsafeToFuture().map(_.asJson).map(Ok(_)))
    }
}

object SlackController {
  final case class Request(token: NonEmptyString, channelName: NonEmptyString, userName: NonEmptyString)

  object Request {
    implicit val decoder: Decoder[Request] = deriveDecoder
  }
}
