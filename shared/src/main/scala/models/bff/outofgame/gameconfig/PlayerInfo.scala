package models.bff.outofgame.gameconfig

import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerInfo.ValidPlayerInfo
import models.bff.outofgame.gameconfig.PlayerStatus.Ready
import utils.misc.RGBColour
import models.syntax.Pointed

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
  import io.circe.*
  import io.circe.generic.semiauto.*
  given Decoder[PlayerInfo] = deriveDecoder[PlayerInfo]
  given Encoder[PlayerInfo] = deriveEncoder[PlayerInfo]

  given Pointed[PlayerInfo] = Pointed.factory(
    PlayerInfo(
      Pointed[PlayerName].unit,
      Pointed[Option[PlayerClasses]].unit,
      Pointed[Option[RGBColour]].unit,
      Pointed[PlayerStatus].unit,
      Pointed[PlayerType].unit
    )
  )

  final case class ValidPlayerInfo(
      playerName: PlayerName,
      playerClass: PlayerClasses,
      playerColour: RGBColour,
      playerType: PlayerType
  ) {
    def status: PlayerStatus = Ready
  }

  object ValidPlayerInfo {
    given Encoder[ValidPlayerInfo] = deriveEncoder
    given Decoder[ValidPlayerInfo] = deriveDecoder
  }

}
