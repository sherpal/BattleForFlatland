package models.users

import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._

object RouteDefinitions {

  final val entry             = root / endOfSegments
  final val loginRoute        = root / "login" / endOfSegments
  final val registerRoute     = root / "register" / endOfSegments
  final val postRegisterRoute = (root / "post-register" / endOfSegments) ? param[String]("userName")
  final val confirmRoute      = (root / "confirm-registration") ? param[String]("registrationKey")

  final val homeRoute = root / "home"

}
