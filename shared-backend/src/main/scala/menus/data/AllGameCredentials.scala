package menus.data

import models.bff.ingame.GameUserCredentials
import io.circe.Codec
import java.nio.charset.StandardCharsets
import scala.util.Try

final case class AllGameCredentials(
    gameCredentials: GameCredentials,
    allGameUserCredentials: Vector[GameUserCredentials]
) {
  def secret: String = gameCredentials.gameSecret
}

object AllGameCredentials {
  given Codec[AllGameCredentials] = io.circe.generic.semiauto.deriveCodec
}
