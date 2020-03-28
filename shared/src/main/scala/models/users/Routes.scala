package models.users

import urldsl.language.PathSegment.dummyErrorImpl._
import urldsl.language.QueryParameters.dummyErrorImpl._

object Routes {

  private val users = root / "users"

  final val confirmRegistration = users / "confirm-registration"
  final val login               = users / "login"
  final val register            = users / "register"

}
