package models.users

import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._

object RouteDefinitions {

  final val userNameParam        = param[String]("userName")
  final val registrationKeyParam = param[String]("registrationKey")

  final val entry             = root / endOfSegments
  final val loginRoute        = root / "login" / endOfSegments
  final val registerRoute     = root / "register" / endOfSegments
  final val postRegisterRoute = (root / "post-register" / endOfSegments) ? userNameParam
  final val confirmRoute      = (root / "confirm-registration") ? registrationKeyParam

  final val registrationKeyFromNameRoute = root / "registration-key-from-name"

  final val homeRoute = root / "home"

  final val testRoute = root / "test"

}
