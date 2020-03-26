package frontend.router

import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._

object RouteDefinitions {

  final val loginRoute    = root / "login"
  final val registerRoute = root / "register"
  final val confirmRoute  = (root / "confirm-registration") ? param[String]("registrationKey")

}
