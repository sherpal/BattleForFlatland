package models.syntax

import models.validators.FieldsValidator

/**
  * A [[Validated]] type `A` with errors `E` is a type whose elements can be
  * validated with a [[models.validators.FieldsValidator]].
  */
trait Validated[A, E] {

  val fieldsValidator: FieldsValidator[A, E]

}

object Validated {

  def apply[A, E](implicit validated: Validated[A, E]): Validated[A, E] = implicitly[Validated[A, E]]

  def factory[A, E](validator: FieldsValidator[A, E]): Validated[A, E] = new Validated[A, E] {
    val fieldsValidator: FieldsValidator[A, E] = validator
  }

}
