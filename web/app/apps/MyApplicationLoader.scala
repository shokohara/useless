package apps

import play.api._

class MyApplicationLoader extends ApplicationLoader {

  @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.Null"))
  private var components: AppComponents = _

  def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    components = new AppComponents(context) with NoHttpFiltersComponents
    components.application
  }
}
