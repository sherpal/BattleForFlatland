package models.bff.outofgame

import java.time.LocalDateTime

import io.circe.generic.auto._
import io.circe.parser.decode
import models.bff.outofgame.gameconfig.GameConfiguration
import models.syntax.Pointed
import models.users.User

final case class DBMenuGame(
    gameId: String,
    gameName: String,
    maybeHashedPassword: Option[String],
    gameCreatorId: String,
    createdOn: LocalDateTime,
    gameConfigurationAsString: String
) {
  def menuGame(creator: User): MenuGame = ???
  // MenuGame(
  //   gameId,
  //   gameName,
  //   maybeHashedPassword,
  //   creator,
  //   createdOn,
  //   decode[GameConfiguration](gameConfigurationAsString).getOrElse(Pointed[GameConfiguration].unit)
  // )
}
