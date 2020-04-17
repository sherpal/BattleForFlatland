package models.bff.ingame

/**
  * Credentials that are given to the game server so that it can ask the game contents to the
  * server when it is launched. This is generated automatically for each game.
  * @param gameId id of the game
  * @param gameSecret auto generated secret for the game server and the server to communicate.
  */
final case class GameCredentials(gameId: String, gameSecret: String)
