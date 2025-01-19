package menus.data

import models.bff.outofgame.PlayerClasses
import io.circe.Codec

final case class RemoveAIFromGame(gameId: String, cls: PlayerClasses)

object RemoveAIFromGame {
  given Codec[RemoveAIFromGame] = io.circe.generic.semiauto.deriveCodec
}
