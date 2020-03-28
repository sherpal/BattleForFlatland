package utils

import errors.ErrorADT
import errors.ErrorADT.{MultipleErrors, MultipleErrorsMap, WrongStatusCode}
import models.validators.{FieldsValidator, Validator}
import zio.{IO, UIO, ZIO}

package object ziohelpers {

  def failIfWith[E](mustFail: => Boolean, e: E): IO[E, Unit] = if (mustFail) ZIO.fail(e) else ZIO.succeed(())

  def unsuccessfulStatusCode(statusCode: Int): ZIO[Any, WrongStatusCode, Unit] =
    UIO(()).filterOrFail(_ => statusCode / 100 != 2)(WrongStatusCode(statusCode))

  def validateOrFail[E <: ErrorADT, T](validator: Validator[T, E])(t: T): IO[ErrorADT, Unit] =
    validator(t) match {
      case Nil         => ZIO.succeed(())
      case head :: Nil => ZIO.fail(head)
      case errors      => ZIO.fail(MultipleErrors(errors))
    }

  def fieldsValidateOrFail[E <: ErrorADT, T](
      fieldsValidator: FieldsValidator[T, E]
  )(t: T): IO[MultipleErrorsMap, Unit] = {
    val errors = fieldsValidator.validate(t)
    if (errors.isEmpty) ZIO.succeed(())
    else ZIO.fail(MultipleErrorsMap(errors))
  }

}
