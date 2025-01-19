package menus.data

import io.circe.Codec
import models.bff.outofgame.gameconfig.GameConfiguration

final case class ChangeGameMetadataFormData(
    gameId: String,
    gameMetadata: GameConfiguration.GameConfigMetadata
)

object ChangeGameMetadataFormData {
  given Codec[ChangeGameMetadataFormData] = io.circe.generic.semiauto.deriveCodec
}
