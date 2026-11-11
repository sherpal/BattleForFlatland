package menus.data

import io.circe.Codec

final case class GameIdFormData(gameId: Int)

object GameIdFormData {
  given Codec[GameIdFormData] = io.circe.generic.semiauto.deriveCodec
}
