package models.syntax

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

}
