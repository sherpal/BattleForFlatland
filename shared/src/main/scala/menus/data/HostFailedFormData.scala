package menus.data

import io.circe.Codec

/** Sent by the player chosen to host the game, when they cannot run the game server. */
final case class HostFailedFormData(gameId: String, reason: String)

object HostFailedFormData {
  given Codec[HostFailedFormData] = io.circe.generic.semiauto.deriveCodec
}
