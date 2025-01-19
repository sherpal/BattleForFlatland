package menus.data

import io.circe.Codec

sealed trait MenuGameComm

object MenuGameComm {
  case class DataUpdated() extends MenuGameComm
  case class GameStarted() extends MenuGameComm
  case class HereAreYourCredentials(gameId: String, secret: String, gameServerPort: Int)
      extends MenuGameComm

  given Codec[MenuGameComm] = io.circe.generic.semiauto.deriveCodec

}
