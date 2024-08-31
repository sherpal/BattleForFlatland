package menus.data

import io.circe.Codec

final case class GameIdFormData(gameId: String)

object GameIdFormData {
  given Codec[GameIdFormData] = io.circe.generic.semiauto.deriveCodec
}
