package models.bff.outofgame

import java.time.LocalDateTime

import errors.ErrorADT
import models.users.User
import models.validators.FieldsValidator
import models.validators.StringValidators._

final case class MenuGame(
    gameId: String,
    gameName: String,
    maybeHashedPassword: Option[String],
    gameCreator: User,
    createdOn: LocalDateTime
) {

  /**
    * Returns this [[MenuGame]] where the password has been "forgotten". The class still knows whether there is a
    * password (if it's `Some("")`) but loses the information.
    */
  def forgetPassword: MenuGame = copy(maybeHashedPassword = maybeHashedPassword.map(_ => ""))
}

object MenuGame {

  def empty: MenuGame = MenuGame("", "", None, User.empty, LocalDateTime.now)

  val validator: FieldsValidator[MenuGame, ErrorADT] = FieldsValidator(
    Map(
      "gameName" -> nonEmptyString.contraMap(_.gameName),
      "password" -> nonEmptyString.toOptionValidator.contraMap(_.maybeHashedPassword)
    )
  )

}
