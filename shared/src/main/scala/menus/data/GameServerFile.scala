package menus.data

import io.circe.Codec

/** What the players download to run the game server (its jar, or a Java runtime), as the menu
  * server has it: its version, its SHA-256 (with which they check what they downloaded), and its
  * size in bytes (for the progress of the download).
  */
final case class GameServerFile(sha256: String, size: Long)

object GameServerFile {
  given Codec[GameServerFile] = io.circe.generic.semiauto.deriveCodec
}
