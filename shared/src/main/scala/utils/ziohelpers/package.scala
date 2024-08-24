package utils

import errors.ErrorADT
import errors.ErrorADT.{MultipleErrors, MultipleErrorsMap, WrongStatusCode}
import models.validators.{FieldsValidator, Validator}
import services.http.HttpClient
import services.localstorage.LocalStorage
import services.logging.Logging
import services.routing.Routing
import services.toaster.Toaster
import zio.{IO, UIO, ZIO}

package object ziohelpers {

  type FrontendGlobalEnv = HttpClient & LocalStorage & Logging & Routing & Toaster

  /** Returns [[Unit]] if `mustFail` is false, and fail with `e` otherwise.
    */
  def failIfWith[E](mustFail: => Boolean, e: => E): IO[E, Unit] = if (mustFail) ZIO.fail(e) else ZIO.succeed(())

  /** Gets the content of the `maybeA` [[Option]], and fail with `e` if it is empty.
    */
  def getOrFail[E, A](maybeA: => Option[A], e: => E): IO[E, A] =
    ZIO.succeed(maybeA).filterOrFail(_.isDefined)(e).map(_.get)

  /** Consistency check that the `statusCode` is a success status code.
    *
    * This method can be called after the return of an http call to be sure that the status code is a success code.
    * Normally, a failure should have happened before.
    */
  def unsuccessfulStatusCode(statusCode: Int): ZIO[Any, WrongStatusCode, Unit] =
    ZIO.when(statusCode / 100 > 3)(ZIO.fail(WrongStatusCode(statusCode))).unit

  /** Validate the input `t` with the given [[Validator]]. If the element fails to be validated with only one error,
    * this error is returned. If it fails with several errors, there are wrapped into a
    * [[errors.ErrorADT.MultipleErrorsMap]].
    */
  def validateOrFail[E <: ErrorADT, T](validator: Validator[T, E])(t: T): IO[ErrorADT, Unit] =
    validator(t) match {
      case Nil         => ZIO.succeed(())
      case head :: Nil => ZIO.fail(head)
      case errors      => ZIO.fail(MultipleErrors(errors))
    }

  /** Applies the given [[models.validators.FieldsValidator]] to the element `t`. If the element fails to pass
    * validation, its [[errors.ErrorADT.MultipleErrorsMap]] is returned. Returns [[Unit]] otherwise.
    */
  def fieldsValidateOrFail[E <: ErrorADT, T](
      fieldsValidator: FieldsValidator[T, E]
  )(t: T): IO[MultipleErrorsMap, Unit] = {
    val errors = fieldsValidator.validate(t)
    if (errors.isEmpty) ZIO.succeed(())
    else ZIO.fail(MultipleErrorsMap(errors))
  }

}
