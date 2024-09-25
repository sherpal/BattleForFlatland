package models.bff.outofgame

import java.time.LocalDateTime

import errors.ErrorADT
import models.bff.outofgame.gameconfig.GameConfiguration
import models.syntax.Validated
import menus.data.User
import models.validators.FieldsValidator
import models.validators.StringValidators.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import models.bff.outofgame.gameconfig.PlayerInfo
import models.bff.outofgame.gameconfig.PlayerType

final case class MenuGame(
    gameId: String,
    gameName: String,
    maybeHashedPassword: Option[String],
    gameCreator: User,
    createdOn: LocalDateTime,
    gameConfiguration: GameConfiguration
) {

  /** Returns this [[MenuGame]] where the password has been "forgotten". The class still knows
    * whether there is a password (if it's `Some("")`) but loses the information.
    */
  def forgetPassword: MenuGame = copy(
    maybeHashedPassword = maybeHashedPassword.map(_ => ""),
    gameCreator = gameCreator
  )

  def onlyCreatorName: MenuGame = this

  def gameConfigurationIsValid: Boolean = gameConfiguration.isValid

  def withPlayer(playerInfo: PlayerInfo): MenuGame = copy(gameConfiguration =
    gameConfiguration.copy(
      playersInfo = gameConfiguration.playersInfo + (playerInfo.playerName.name -> playerInfo)
    )
  )

  def removePlayer(playerName: String): MenuGame = copy(
    gameConfiguration = gameConfiguration.copy(
      playersInfo = gameConfiguration.playersInfo - playerName
    )
  )

  def removeAllAIs: MenuGame = copy(
    gameConfiguration = gameConfiguration.copy(
      playersInfo = gameConfiguration.playersInfo.collect {
        case (name, info) if info.playerType != PlayerType.ArtificialIntelligence => (name, info)
      }
    )
  )

}

object MenuGame {

  val validator: FieldsValidator[MenuGame, ErrorADT] = FieldsValidator(
    Map(
      "gameName" -> nonEmptyString.contraMap(_.gameName),
      "password" -> nonEmptyString.toOptionValidator.contraMap(_.maybeHashedPassword)
    )
  )

  given Validated[MenuGame, ErrorADT] = Validated.factory(validator)

  given Encoder[MenuGame] = deriveEncoder
  given Decoder[MenuGame] = deriveDecoder

}
