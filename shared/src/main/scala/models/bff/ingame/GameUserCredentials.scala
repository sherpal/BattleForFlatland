package models.bff.ingame

/** Allows a user to connect to the game server. In order to connect to the game server, the user
  * must provide the server with the correct credentials. Those consist of the user id, the game id,
  * and a unique secret generated for the user.
  *
  * The game server checks upon connection that user gives the correct secret for the correct game.
  *
  * @param userId
  *   global id of the user
  * @param gameId
  *   id of the current game
  * @param userSecret
  *   per-game generated secret
  */
final case class GameUserCredentials(userName: String, gameId: String, userSecret: String)

object GameUserCredentials {
  import io.circe.Codec
  import io.circe.generic.semiauto.deriveCodec
  given Codec[GameUserCredentials] = deriveCodec

  final def tupled: ((String, String, String)) => GameUserCredentials = (apply(_, _, _)).tupled
}
