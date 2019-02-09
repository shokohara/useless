package controllers

import cats.effect.IO
import com.github.shokohara.slack.{ApplicationConfig, Hello}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext
import scala.util.chaining._

class SlackController(cc: ControllerComponents)(implicit val ec: ExecutionContext)
  extends AbstractController(cc) with Circe {
  import SlackController._

  def index = Action.async(circe.json[Request]) { request =>
    import io.circe.generic.auto._
    ApplicationConfig(request.body.token, request.body.channelName, request.body.userName)
      .pipe(Hello.f).pipe(_.flatMap(_.fold(IO.raiseError, IO.pure)).unsafeToFuture().map(_.asJson).map(Ok(_)))
  }
}

object SlackController {
  final case class Request(token: String, channelName: String, userName: String)

  object Request {
    implicit val decoder: Decoder[Request] = deriveDecoder
  }
}
