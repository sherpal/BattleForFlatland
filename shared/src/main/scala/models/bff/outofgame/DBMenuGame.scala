package models.bff.outofgame

import java.time.LocalDateTime

import models.users.User

final case class DBMenuGame(
    gameId: String,
    gameName: String,
    maybeHashedPassword: Option[String],
    gameCreatorId: String,
    createdOn: LocalDateTime
) {
  def menuGame(creator: User): MenuGame = MenuGame(
    gameId,
    gameName,
    maybeHashedPassword,
    creator,
    createdOn
  )
}
