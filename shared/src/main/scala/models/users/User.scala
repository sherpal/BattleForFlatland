package models.users

import java.time.LocalDateTime

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

}
