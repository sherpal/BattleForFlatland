package models.users

import errors.ErrorADT
import errors.ErrorADT.PasswordsMismatch
import models.validators.StringValidators.{
  atLeastLength,
  containsDigit,
  containsLowercase,
  containsUppercase,
  doesNotContainAnyOf,
  emailValidator,
  nonEmptyString,
  validPassword
}
import models.validators.{FieldsValidator, Validator}
import models.validators.Validator.simpleValidator
import models.validators.StringValidators.noSpace

final case class NewUser(name: String, password: String, confirmPassword: String, email: String) {
  def valid: Boolean = password == confirmPassword

  /**
    * Returns a number between 0 and 1 indicating the strentgh of the `password`.
    * 0 means weak, while 1 means strong.
    *
    * We define a number of criteria that we want to impose on a string password, via validators.
    * The strength is the relative number of criteria that pass.
    */
  def passwordStrength: Double = {
    val criteria: List[Validator[String, Any]] = List(
      atLeastLength(8),
      containsDigit,
      containsLowercase,
      containsUppercase
    )

    criteria.count(_(password).isEmpty).toDouble / criteria.length
  }

}

object NewUser {

  def validate(newUser: NewUser): List[ErrorADT] = validator(newUser)

  def samePasswords: Validator[NewUser, ErrorADT] =
    simpleValidator[NewUser, ErrorADT](
      _.valid,
      _ => PasswordsMismatch
    )

  def validator: Validator[NewUser, ErrorADT] =
    nonEmptyString.contraMap[NewUser](_.name) ++
      atLeastLength(4).contraMap[NewUser](_.name) ++
      validPassword.contraFlatMap[NewUser](user => List(user.password, user.confirmPassword)) ++
      samePasswords

  def fieldsValidator: FieldsValidator[NewUser, ErrorADT] =
    FieldsValidator(
      Map(
        "name" -> (nonEmptyString ++ atLeastLength(4) ++ noSpace ++
          doesNotContainAnyOf(List("?", "@", ":", "&", "$", "<", ">", ",", "!", "§", "`", "$")))
          .contraMap[NewUser](_.name),
        "password" -> validPassword.contraMap[NewUser](_.password),
        "confirmPassword" -> validPassword.contraMap[NewUser](_.confirmPassword),
        "passwordMatch" -> samePasswords,
        "email" -> emailValidator.contraMap[NewUser](_.email)
      )
    )

}
