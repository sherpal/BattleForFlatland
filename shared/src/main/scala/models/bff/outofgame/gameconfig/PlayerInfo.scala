package models.bff.outofgame.gameconfig

import models.bff.outofgame.PlayerClasses
import models.bff.outofgame.gameconfig.PlayerInfo.ValidPlayerInfo
import models.bff.outofgame.gameconfig.PlayerStatus.Ready
import utils.misc.RGBColour
import models.syntax.Pointed
import models.validators.Validator

final case class PlayerInfo(
    playerName: PlayerName,
    maybePlayerClass: Option[PlayerClasses],
    maybePlayerColour: Option[RGBColour],
    status: PlayerStatus,
    playerType: PlayerType
) {
  def isReady: Boolean = status == Ready
  def isValid: Boolean = PlayerInfo.playerInfoValidator.isValid(this)
  def asValid: Either[List[String], ValidPlayerInfo] =
    for {
      _ <- PlayerInfo.playerInfoValidator.validate(this) match {
        case Nil    => Right(())
        case errors => Left(errors)
      }
      playerClass  <- maybePlayerClass.toRight(List("Class is empty"))
      playerColour <- maybePlayerColour.toRight(List("Colour is empty"))
      _            <- Either.cond(isReady, (), List("Not Ready"))
    } yield ValidPlayerInfo(playerName, playerClass, playerColour, playerType)

  def withHumanName(name: String): PlayerInfo = copy(playerName = PlayerName.HumanPlayerName(name))

  def withColour(colour: RGBColour): PlayerInfo = copy(maybePlayerColour = Some(colour))

  def withClass(cls: Option[PlayerClasses]): PlayerInfo = copy(maybePlayerClass = cls)

  def withReadyState(readyStatus: PlayerStatus): PlayerInfo = copy(status = readyStatus)
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

  val playerInfoValidator =
    Validator
      .mustBeDefinedValidator("Class is required.")
      .contraMap[PlayerInfo](_.maybePlayerClass) ++
      Validator
        .mustBeDefinedValidator("Colour is required.")
        .contraMap[PlayerInfo](_.maybePlayerColour) ++
      Validator
        .simpleValidator(
          (status: PlayerStatus) => status == PlayerStatus.Ready,
          _ => "Player is not ready"
        )
        .contraMap[PlayerInfo](_.status)

}
