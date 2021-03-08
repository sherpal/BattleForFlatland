package models.bff.outofgame.gameconfig

import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerInfo.ValidPlayerInfo
import models.bff.outofgame.gameconfig.PlayerStatus.Ready
import utils.misc.RGBColour

final case class PlayerInfo(
    playerName: PlayerName,
    maybePlayerClass: Option[PlayerClasses],
    maybePlayerColour: Option[RGBColour],
    status: PlayerStatus,
    playerType: PlayerType
) {
  def isReady: Boolean = status == Ready
  def isValid: Boolean = asValid.isDefined
  def asValid: Option[ValidPlayerInfo] =
    for {
      playerClass  <- maybePlayerClass
      playerColour <- maybePlayerColour
      if isReady
    } yield ValidPlayerInfo(playerName, playerClass, playerColour, playerType)
}

object PlayerInfo {
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[PlayerInfo] = deriveDecoder[PlayerInfo]
  implicit val fooEncoder: Encoder[PlayerInfo] = deriveEncoder[PlayerInfo]

  final case class ValidPlayerInfo(
      playerName: PlayerName,
      playerClass: PlayerClasses,
      playerColour: RGBColour,
      playerType: PlayerType
  ) {
    def status: PlayerStatus = Ready
  }

}
