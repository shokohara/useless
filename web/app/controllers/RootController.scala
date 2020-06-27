package controllers

import java.time.{ZoneId, ZonedDateTime}

import io.circe.refined._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class RootController(cc: ControllerComponents) extends AbstractController(cc) {

  def index: Action[AnyContent] =
    Action {
      Ok(views.html.Index.render(ZonedDateTime.now(ZoneId.of("Asia/Tokyo")).getYear))
    }
}
