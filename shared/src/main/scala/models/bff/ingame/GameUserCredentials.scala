package models.bff.ingame

/**
  * Allows a user to connect to the game server.
  * In order to connect to the game server, the user must provide the server with the correct credentials.
  * Those consist of the user id, the game id, and a unique secret generated for the user.
  *
  * The game server checks upon connection that user gives the correct secret for the correct game.
  *
  * @param userId global id of the user
  * @param gameId id of the current game
  * @param userSecret per-game generated secret
  */
final case class GameUserCredentials(userId: String, gameId: String, userSecret: String)

object GameUserCredentials {
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[GameUserCredentials] = deriveDecoder[GameUserCredentials]
  implicit val fooEncoder: Encoder[GameUserCredentials] = deriveEncoder[GameUserCredentials]

  final def tupled: ((String, String, String)) => GameUserCredentials = (apply _).tupled
}
