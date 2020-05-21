package models.syntax

import java.time.LocalDateTime

/**
  * A Pointed type `A` is a type for which there exists at least one element.
  * This is the case for all types except Nothing, but not all types have a
  * special point.
  */
trait Pointed[A] {
  def unit: A
}

object Pointed {

  def apply[A](implicit pointed: Pointed[A]): Pointed[A] = implicitly[Pointed[A]]

  def factory[A](a: A): Pointed[A] = new Pointed[A] {
    def unit: A = a
  }

  import magnolia._
  import scala.language.experimental.macros

  type Typeclass[A] = Pointed[A]

  def combine[A](ctx: CaseClass[Pointed, A]): Pointed[A] = new Pointed[A] {
    def unit: A = ctx.construct(_.typeclass.unit)
  }

  def dispatch[A](ctx: SealedTrait[Pointed, A]): Pointed[A] = new Pointed[A] {
    def unit: A = ctx.subtypes.head.typeclass.unit
  }

  implicit def derivePointed[A]: Pointed[A] = macro Magnolia.gen[A]

  implicit val stringPointed: Pointed[String]                              = factory("")
  implicit def numericPointed[A](implicit numeric: Numeric[A]): Pointed[A] = factory(numeric.zero)
  implicit def localDateTimePointed: Pointed[LocalDateTime]                = factory(LocalDateTime.now)
  implicit def booleanPointed: Pointed[Boolean] = factory(false) // This is though.

  implicit def listPointed[A]: Pointed[List[A]]     = factory(Nil)
  implicit def mapPointed[K, V]: Pointed[Map[K, V]] = factory(Map())
  implicit def optionPointed[A]: Pointed[Option[A]] = factory(None)

}
