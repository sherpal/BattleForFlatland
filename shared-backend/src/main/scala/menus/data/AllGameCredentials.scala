package menus.data

import io.circe.Codec
import models.bff.ingame.GameUserCredentials

final case class AllGameCredentials(
    gameCredentials: GameCredentials,
    allGameUserCredentials: Vector[GameUserCredentials]
) {
  def secret: String = gameCredentials.gameSecret
}

object AllGameCredentials {
  given Codec[AllGameCredentials] = io.circe.generic.semiauto.deriveCodec
}
