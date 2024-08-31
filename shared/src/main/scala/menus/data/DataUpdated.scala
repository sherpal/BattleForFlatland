package menus.data

import io.circe.Codec

final case class DataUpdated()

object DataUpdated {
  given Codec[DataUpdated] = io.circe.generic.semiauto.deriveCodec
}
