package menus.data

import io.circe.Codec

case class GameStarted()

object GameStarted {
  given Codec[GameStarted] = io.circe.generic.semiauto.deriveCodec
}