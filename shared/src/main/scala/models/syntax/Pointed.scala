package models.syntax

import java.time.LocalDateTime

import magnolia1.*

/** A Pointed type `A` is a type for which there exists at least one element. This is the case for all types except
  * Nothing, but not all types have a special point.
  */
trait Pointed[A] {
  def unit: A
}

object Pointed extends AutoDerivation[Pointed] {

  def apply[A](using pointed: Pointed[A]): Pointed[A] = pointed

  def factory[A](a: A): Pointed[A] = new Pointed[A] {
    def unit: A = a
  }

  type Typeclass[A] = Pointed[A]

  def join[A](ctx: CaseClass[Pointed, A]): Pointed[A] = new Pointed[A] {
    def unit: A = ctx.construct(param => param.default.getOrElse(param.typeclass.unit))
  }

  def split[A](ctx: SealedTrait[Pointed, A]): Pointed[A] = new Pointed[A] {
    def unit: A = ctx.subtypes.head.typeclass.unit
  }

  given Pointed[String]                            = factory("")
  given [A](using numeric: Numeric[A]): Pointed[A] = factory(numeric.zero)
  given Pointed[LocalDateTime]                     = factory(LocalDateTime.now)
  given Pointed[Boolean]                           = factory(false) // This is though.

  given [A]: Pointed[List[A]]      = factory(Nil)
  given [K, V]: Pointed[Map[K, V]] = factory(Map())
  given [A]: Pointed[Option[A]]    = factory(None)

}
