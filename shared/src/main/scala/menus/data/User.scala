package menus.data

import io.circe.Codec

final case class User(name: String)

object User {
  given Codec[User] = io.circe.generic.semiauto.deriveCodec
}
