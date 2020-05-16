package models.bff.gameantichamber

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}
import models.bff.ingame.GameUserCredentials
import models.bff.outofgame.gameconfig.PlayerInfo

sealed trait WebSocketProtocol

object WebSocketProtocol {

  case object GameStatusUpdated extends WebSocketProtocol
  case object GameCancelled extends WebSocketProtocol
  case object HeartBeat extends WebSocketProtocol
  case class PlayerLeavesGame(userId: String) extends WebSocketProtocol
  case class GameUserCredentialsWrapper(gameUserCredentials: GameUserCredentials) extends WebSocketProtocol
  case class UpdateMyInfo(userId: String, playerInfo: PlayerInfo) extends WebSocketProtocol

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i_game_anti_chamber")

  implicit def decoder: Decoder[WebSocketProtocol] = deriveConfiguredDecoder
  implicit def encoder: Encoder[WebSocketProtocol] = deriveConfiguredEncoder

}
