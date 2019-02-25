package controllers

import io.circe.java8.time._
import io.circe.refined._
import play.api.mvc.{AbstractController, ControllerComponents}

class RootController(cc: ControllerComponents) extends AbstractController(cc) {
  def index = TODO
}
