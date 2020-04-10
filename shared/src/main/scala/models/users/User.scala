package models.users

import java.time.LocalDateTime

import models.syntax.Pointed

final case class User(
    userId: String,
    userName: String,
    hashedPassword: String,
    mailAddress: String,
    createdOn: LocalDateTime,
    roles: List[Role]
) {

  /**
    * Copy this [[User]] and forgets the hashed password.
    * The frontend is usually not supposed to have access to it.
    */
  def forgetPassword: User = copy(hashedPassword = "")

  def onlyName: User = User.empty.copy(userId = userId, userName = userName)

}

object User {

  def empty: User = User(
    "",
    "",
    "",
    "",
    LocalDateTime.now,
    Nil
  )

  implicit val pointed: Pointed[User] = Pointed.factory(empty)

}
