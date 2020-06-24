package apps

import _root_.controllers.{AssetsComponents, BuildInfoController, RootController, SlackController}
import com.softwaremill.macwire._
import play.api._
import play.api.http.{HttpErrorHandler, JsonHttpErrorHandler}
import play.api.routing.Router
import router.Routes

abstract class AppComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context) with AssetsComponents {

  override lazy val httpErrorHandler: HttpErrorHandler =
    new JsonHttpErrorHandler(environment, devContext.map(_.sourceMapper))
  lazy val rootController: RootController = wire[RootController]
  lazy val slackController: SlackController = wire[SlackController]
  lazy val buildInfoController: BuildInfoController = wire[BuildInfoController]

  lazy val router: Router =
    new Routes(httpErrorHandler, rootController, slackController, buildInfoController, assets, "/")
}
