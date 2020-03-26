package models.users

import errors.ErrorADT
import models.validators.FieldsValidator
import models.validators.StringValidators.nonEmptyString

final case class LoginUser(userName: String, password: String)

object LoginUser {

  def empty: LoginUser = LoginUser("", "")

  def validator: FieldsValidator[LoginUser, ErrorADT] =
    FieldsValidator(
      Map(
        "name" -> nonEmptyString.contraMap[LoginUser](_.userName),
        "password" -> nonEmptyString.contraMap[LoginUser](_.password)
      )
    )

}
