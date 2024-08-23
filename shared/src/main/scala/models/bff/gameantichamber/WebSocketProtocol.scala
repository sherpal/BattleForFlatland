package models.bff.gameantichamber

import io.circe.{Decoder, Encoder}
import models.bff.ingame.GameUserCredentials
import models.bff.outofgame.gameconfig.PlayerInfo

sealed trait WebSocketProtocol

object WebSocketProtocol {

  case object GameStatusUpdated                                                   extends WebSocketProtocol
  case object GameCancelled                                                       extends WebSocketProtocol
  case object HeartBeat                                                           extends WebSocketProtocol
  case class PlayerLeavesGame(userId: String)                                     extends WebSocketProtocol
  case class GameUserCredentialsWrapper(gameUserCredentials: GameUserCredentials) extends WebSocketProtocol
  case class UpdateMyInfo(userId: String, playerInfo: PlayerInfo)                 extends WebSocketProtocol
  case class UpdateBossName(newBossName: String)                                  extends WebSocketProtocol
  sealed trait ToggleAI                                                           extends WebSocketProtocol
  case object ChooseAIs                                                           extends ToggleAI
  case object ChooseHumans                                                        extends ToggleAI

  /** Sent to everybody when the game creator has clicked on the "Launch Game" button. */
  case object GameLaunched extends WebSocketProtocol

  given Decoder[WebSocketProtocol] = io.circe.generic.semiauto.deriveDecoder
  given Encoder[WebSocketProtocol] = io.circe.generic.semiauto.deriveEncoder

}
