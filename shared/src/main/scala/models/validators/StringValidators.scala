package models.validators

import errors.ErrorADT
import errors.ErrorADT.{ContainsNonLowercaseAlphabet, ShouldContain, ShouldNotContain, StringIsEmpty, StringIsTooShort}
import models.validators.Validator._

object StringValidators {

  private val sv = simpleValidator[String, ErrorADT] _

  final val alphabet = "abcdefghijklmnopqrstuvwxyz"

  final val nonEmptyString = sv(_.nonEmpty, _ => StringIsEmpty)

  final val onlyLowercaseLetters =
    sv(_.forall(c => alphabet.exists(_ == c)), ContainsNonLowercaseAlphabet)

  def atLeastLength(n: Int): Validator[String, ErrorADT] =
    sv(_.length >= n, s => StringIsTooShort(s, n))

  def stringContains(substr: String): Validator[String, ErrorADT] =
    sv(_.contains(substr), ShouldContain(substr, _))

  def stringDoesNotContains(substr: String): Validator[String, ErrorADT] =
    sv(!_.contains(substr), ShouldNotContain(substr, _))

  def doesNotContainAnyOf(
      substrings: List[String],
      errorKey: String = "validator.shouldNotContain"
  ): Validator[String, ErrorADT] =
    substrings
      .map(stringDoesNotContains)
      .foldLeft[Validator[String, ErrorADT]](allowAllValidator)(_ ++ _)

  final val containsUppercase = sv(_.exists(_.isUpper), ShouldContain("upperCase", _))

  final val containsLowercase = sv(_.exists(_.isLower), ShouldContain("lowerCase", _))

  final val containsDigit = sv(t => """\d""".r.findFirstIn(t).isDefined, ShouldContain("digit", _))

  final val noSpace = stringDoesNotContains(" ")

  final val validPassword = nonEmptyString

  final val emailValidator = stringContains("@") ++ noSpace

}
