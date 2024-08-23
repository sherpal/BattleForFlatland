package models.validators

/**
  * A Validator validates elements of type `T`, returning a list of errors of type `E`.
  *
  * For example, a `Validator[String, BackendError]` could implement validate as
  * object NonEmptyStringValidator extends Validator[String, BackendError] {
  *   def validate(t: String): List[BackendError] = List(
  *     Some(BackendError("validator.nonEmptyString", "")).filter(_ => t.nonEmpty)
  *   ).flatten
  * }
  *
  * @tparam T type of elements this validator can validate
  * @tparam E type of errors this validator returns
  */
sealed trait Validator[-T, +E] {

  import Validator._

  /**
    * Methods that generate errors from inputs. A valid input must return the empty list.
    */
  def validate(t: T): List[E]

  /**
    * Validates the input, returning the errors it generates (if any).
    * @param t element to validate
    * @return errors it generates
    */
  final def apply(t: T): List[E] = validate(t)

  /**
    * Concatenates the errors from this validator with the errors from that validator.
    * @param that validator to compose this with
    */
  final def ++[U <: T, F >: E](that: Validator[U, F]): Validator[U, F] =
    validator(t => this.validate(t) ++ that.validate(t))

  /**
    * Transform this validator into a validator for inputs of type `A`. The new validator first applies the
    * function `f` to an `A` and then validates with this validator.
    *
    * @example
    *          if you have a `case class User(name: String, pw: String)`, and a validator for non empty strings called
    *          `nonEmpty`, you can create a validator for `User` with `nonEmpty.contraMap[User](_.name)`
    *
    * @param f function to generate T's from A's
    * @tparam A type for the new validator
    * @return a validator for type `A`
    */
  final def contraMap[A](f: A => T): Validator[A, E] = validator((validate(_)).compose(f))

  /**
    * Same as `contraMap`, but allows to generate several T's at once.
    */
  final def contraFlatMap[A](f: A => List[T]): Validator[A, E] = validator(f(_).flatMap(validate))

  final def maybeContraMap[A, F >: E](f: A => Option[T], error: => F): Validator[A, F] =
    validator(f(_).map(validate).getOrElse(List(error)))

  final def bypassValidator[U <: T](predicate: U => Boolean): Validator[U, E] =
    validator((u: U) => if (predicate(u)) Nil else validate(u))

  /**
    * Creates a [[Validator]] for Options from this validator.
    * See Validator.optionValidator for argument `noneError`.
    */
  final def toOptionValidator[F >: E](noneError: => Option[F]): Validator[Option[T], F] =
    optionValidator(this, noneError)

  /**
    * Creates a [[Validator]] for Options from this validator.
    * None are valid. If you want Nones to be invalid, see method above.
    */
  final def toOptionValidator: Validator[Option[T], E] = toOptionValidator[E](None)

}

object Validator {

  /**
    * Helper factory for generating validators.
    * @param validation validation function for the validator
    */
  def validator[T, E](validation: T => List[E]): Validator[T, E] = new Validator[T, E] {
    def validate(t: T): List[E] = validation(t)
  }

  /**
    * Helper factory for generating simple validators that are only able to generate one or zero error for the
    * inputs.
    * @param predicate predicate that inputs must satisfy to *not* generate errors.
    * @param error error generated by a wrong input
    */
  def simpleValidator[T, E](predicate: T => Boolean, error: T => E): Validator[T, E] =
    validator[T, E](t => if (predicate(t)) Nil else List(error(t)))

  /**
    * Returns a validator that accepts any input.
    */
  def allowAllValidator: Validator[Any, Nothing] = validator(_ => Nil)

  /**
    * Given a validator for a type `T`, creates a validator for type `Option[T]` that returns the `noneError` for None
    * input. If `noneError` is None, None are valid.
    * @param tValidator [[Validator]] for the underlying type
    * @param noneError Error for None input, or None if None input are valid
    * @return [[Validator]] for `Option[T]`
    */
  def optionValidator[T, E](tValidator: Validator[T, E], noneError: Option[E]): Validator[Option[T], E] =
    validator {
      case None        => List(noneError).flatten
      case Some(value) => tValidator(value)
    }

  /**
    * Returns a validator for lists that checks whether a particular element is in the list
    * @param t element to create the validator
    * @param error error produced if `t` is not in the list
    */
  def containsValidator[T, E](t: T, error: List[T] => E): Validator[List[T], E] = simpleValidator(_.contains(t), error)

}
