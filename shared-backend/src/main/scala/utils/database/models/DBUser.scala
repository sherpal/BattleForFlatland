package utils.database.models

import java.time.LocalDateTime

import models.users
import models.users.{Role, User}

final case class DBUser(
    userId: String,
    userName: String,
    hashedPassword: String,
    mailAddress: String,
    createdOn: LocalDateTime
) {
  def user(roles: List[Role]): User = roles match {
    case Nil => users.User(userId, userName, hashedPassword, mailAddress, createdOn, List(Role.SimpleUser))
    case _   => users.User(userId, userName, hashedPassword, mailAddress, createdOn, roles)
  }
}
