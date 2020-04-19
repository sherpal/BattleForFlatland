package models.bff.gameantichamber

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder}
import models.bff.ingame.GameUserCredentials

sealed trait WebSocketProtocol

object WebSocketProtocol {

  private object MoreImplicits {
    import io.circe.generic.auto._

    def gameUserCredentialsEncoder: Encoder[GameUserCredentials] = implicitly[Encoder[GameUserCredentials]]
    def gameUserCredentialsDecoder: Decoder[GameUserCredentials] = implicitly[Decoder[GameUserCredentials]]
  }

  private implicit def gameUserCredentialsEncoder: Encoder[GameUserCredentials] =
    MoreImplicits.gameUserCredentialsEncoder
  private implicit def gameUserCredentialsDecoder: Decoder[GameUserCredentials] =
    MoreImplicits.gameUserCredentialsDecoder

  case object GameStatusUpdated extends WebSocketProtocol
  case object GameCancelled extends WebSocketProtocol
  case object HeartBeat extends WebSocketProtocol
  case class PlayerLeavesGame(userId: String) extends WebSocketProtocol
  case class GameUserCredentialsWrapper(gameUserCredentials: GameUserCredentials) extends WebSocketProtocol

  import io.circe.generic.extras.semiauto._
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("what_am_i_game_anti_chamber")

  implicit def decoder: Decoder[WebSocketProtocol] = deriveConfiguredDecoder
  implicit def encoder: Encoder[WebSocketProtocol] = deriveConfiguredEncoder

}
