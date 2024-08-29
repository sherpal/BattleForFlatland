package models.bff.ingame

import models.bff.outofgame.MenuGameWithPlayers

/** Contains all the credentials for every user, and all information about the game
  */
final case class GameCredentialsWithGameInfo(
    allGameCredentials: AllGameCredentials,
    gameInfo: MenuGameWithPlayers
) {

  /** Checks if the amount of users and credentials match.
    */
  def isValid: Boolean =
    allGameCredentials.allGameUserCredentials.map(_.userName).sorted == gameInfo.players
      .map(_.name)
      .sorted

}
