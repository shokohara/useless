package controllers

import io.circe.java8.time._
import io.circe.refined._
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, ControllerComponents}

class RootController(cc: ControllerComponents, assets: Assets) extends AbstractController(cc) with Circe {
  def index = assets.at("index.html")
}
