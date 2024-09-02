package menus.data

import models.bff.outofgame.MenuGameWithPlayers
import io.circe.Codec
import java.nio.charset.StandardCharsets
import scala.util.Try

/** Contains all the credentials for every user, and all information about the game
  */
final case class GameCredentialsWithGameInfo(
    allGameCredentials: AllGameCredentials,
    gameInfo: MenuGameWithPlayers
) {

  def secret: String = allGameCredentials.secret

  /** Checks if the amount of users and credentials match.
    */
  def isValid: Boolean =
    allGameCredentials.allGameUserCredentials.map(_.userName).sorted == gameInfo.players
      .map(_.name)
      .sorted

}

object GameCredentialsWithGameInfo {
  given Codec[GameCredentialsWithGameInfo] = io.circe.generic.semiauto.deriveCodec

  def encode(creds: GameCredentialsWithGameInfo): String = java.util.Base64
    .getEncoder()
    .encodeToString(
      Codec[GameCredentialsWithGameInfo].apply(creds).noSpaces.getBytes(StandardCharsets.UTF_8)
    )

  def decode(str: String): Either[Throwable, GameCredentialsWithGameInfo] = for {
    jsonStr <- Try(
      new String(java.util.Base64.getDecoder().decode(str), StandardCharsets.UTF_8)
    ).toEither
    creds <- io.circe.parser.decode[GameCredentialsWithGameInfo](jsonStr)
  } yield creds
}
