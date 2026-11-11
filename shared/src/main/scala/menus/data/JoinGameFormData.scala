package menus.data

import io.circe.Codec

final case class JoinGameFormData(
    gameId: Int,
    maybePassword: Option[String]
)

object JoinGameFormData {
  given Codec[JoinGameFormData] = io.circe.generic.semiauto.deriveCodec
}
