package models.validators

import errors.ErrorADT
import errors.ErrorADT.{Negative, NonZero, NotBiggerThan, NotSmallerThan}
import models.validators.Validator._

sealed trait NumericValidators[T] {

  val num: Numeric[T]

  private def sv: (T => Boolean, T => ErrorADT) => Validator[T, ErrorADT] = simpleValidator[T, ErrorADT]

  def nonZero: Validator[T, ErrorADT] = sv(_ != num.zero, t => NonZero(t.toString))

  def biggerThan(t: T): Validator[T, ErrorADT] =
    sv(num.compare(_, t) > 0, x => NotBiggerThan(x.toString, t.toString))

  def lessThan(t: T): Validator[T, ErrorADT] =
    sv(num.compare(_, t) < 0, x => NotSmallerThan(x.toString, t.toString))

  def nonNegative: Validator[T, ErrorADT] =
    sv(num.compare(_, num.zero) > 0, t => Negative(t.toString))

  def positive: Validator[T, ErrorADT] = nonZero ++ nonNegative

  /** Between `lower` and `upper` included */
  def between(lower: T, upper: T): Validator[T, ErrorADT] = {
    import num.mkNumericOps
    biggerThan(lower - num.one) ++ lessThan(upper + num.one)
  }

  /** Between `lower` and `upper` excluded */
  def within(lower: T, upper: T): Validator[T, ErrorADT] = {
    import num.mkNumericOps
    between(lower + num.one, upper - num.one)
  }

}

object NumericValidators {

  def apply[T](implicit n: Numeric[T]): NumericValidators[T] = new NumericValidators[T] {
    val num: Numeric[T] = n
  }

  implicit def invoke[T](implicit num: Numeric[T]): NumericValidators[T] = apply[T]

}
