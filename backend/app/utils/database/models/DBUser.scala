package utils.database.models

import java.time.LocalDateTime

import models.{Role, User}

final case class DBUser(
    userId: String,
    userName: String,
    hashedPassword: String,
    mailAddress: String,
    createdOn: LocalDateTime
) {
  def user(roles: List[Role]): User = roles match {
    case Nil => User(userId, userName, hashedPassword, mailAddress, createdOn, List(Role.SimpleUser))
    case _   => User(userId, userName, hashedPassword, mailAddress, createdOn, roles)
  }
}
