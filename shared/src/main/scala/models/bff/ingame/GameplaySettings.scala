package models.bff.ingame

import models.bff.ingame.GameplaySettings.Volume
import io.circe.Codec
import models.syntax.Pointed

final case class GameplaySettings(
    volume: Volume
)

object GameplaySettings {

  case class Volume(value: Int) // should be between 0 and 10 (incl)

  object Volume {
    given Pointed[Volume] = Pointed.factory(Volume(5))
  }

  private given Codec[Volume] = io.circe.generic.semiauto.deriveCodec

  given Codec[GameplaySettings] = io.circe.generic.semiauto.deriveCodec

}
