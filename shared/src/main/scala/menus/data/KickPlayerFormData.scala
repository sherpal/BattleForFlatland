package menus.data

import io.circe.Codec

final case class KickPlayerFormData(gameId: String, playerName: String)

object KickPlayerFormData {
  given Codec[KickPlayerFormData] = io.circe.generic.semiauto.deriveCodec
}
