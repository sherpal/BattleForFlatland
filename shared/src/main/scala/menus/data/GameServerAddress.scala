package menus.data

import io.circe.Codec

/** An address (IP address and UDP port) where the game server may be reached. */
final case class GameServerAddress(host: String, port: Int)

object GameServerAddress {
  given Codec[GameServerAddress] = io.circe.generic.semiauto.deriveCodec
}
