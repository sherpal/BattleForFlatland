package models.common

import models.syntax.Pointed

/**
  * Simple wrapper to send a password in the body of a request.
  * If password is not required, None can be provided.
  */
final case class PasswordWrapper(submittedPassword: Option[String])

object PasswordWrapper {

  implicit val pointed: Pointed[PasswordWrapper] = Pointed.factory(PasswordWrapper(None))

}
