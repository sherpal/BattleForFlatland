package menus.data

import io.circe.Codec

final case class CreateGameFormData(gameName: String)

object CreateGameFormData {
  given Codec[CreateGameFormData] = io.circe.generic.semiauto.deriveCodec
}
