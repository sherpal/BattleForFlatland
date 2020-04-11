package models.bff.gameantichamber

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}

sealed trait WebSocketProtocol

object WebSocketProtocol {

  case object GameStatusUpdated extends WebSocketProtocol
  case object GameCancelled extends WebSocketProtocol
  case object HeartBeat extends WebSocketProtocol

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i_game_anti_chamber")

  implicit def decoder: Decoder[WebSocketProtocol] = deriveConfiguredDecoder
  implicit def encoder: Encoder[WebSocketProtocol] = deriveConfiguredEncoder

}
