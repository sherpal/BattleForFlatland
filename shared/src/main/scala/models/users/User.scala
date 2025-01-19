package models.users

import java.time.LocalDateTime

import models.syntax.Pointed
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class User(
    userId: String,
    userName: String,
    hashedPassword: String,
    mailAddress: String,
    createdOn: LocalDateTime,
    roles: List[Role]
) {

  /** Copy this [[User]] and forgets the hashed password. The frontend is usually not supposed to have access to it.
    */
  def forgetPassword: User = copy(hashedPassword = "")

  def onlyName: User = Pointed[User].unit.copy(userId = userId, userName = userName)

}

object User {
  given Decoder[User] = deriveDecoder
  given Encoder[User] = deriveEncoder
}
