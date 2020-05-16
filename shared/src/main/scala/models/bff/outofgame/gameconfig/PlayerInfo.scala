package models.bff.outofgame.gameconfig

import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerStatus.Ready
import utils.misc.RGBColour

final case class PlayerInfo(
    playerName: String,
    playerClass: PlayerClasses,
    playerColour: RGBColour,
    status: PlayerStatus
) {
  def isReady: Boolean = status == Ready
}

object PlayerInfo {
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[PlayerInfo] = deriveDecoder[PlayerInfo]
  implicit val fooEncoder: Encoder[PlayerInfo] = deriveEncoder[PlayerInfo]

}
