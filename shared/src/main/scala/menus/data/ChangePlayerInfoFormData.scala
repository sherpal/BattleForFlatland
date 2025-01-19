package menus.data

import models.bff.outofgame.gameconfig.PlayerInfo
import io.circe.Codec

final case class ChangePlayerInfoFormData(gameId: String, playerInfo: PlayerInfo)

object ChangePlayerInfoFormData {
  given Codec[ChangePlayerInfoFormData] = io.circe.generic.semiauto.deriveCodec
}
