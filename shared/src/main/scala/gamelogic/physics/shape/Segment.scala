package gamelogic.physics.shape

import gamelogic.physics.Complex

final case class Segment(z1: Complex, z2: Complex) {

  def intersectionPoint(other: Segment): Option[Complex] =
    Shape.intersectionPoint(
      z1.re,
      z1.im,
      z2.re,
      z2.im,
      other.z1.re,
      other.z1.im,
      other.z2.re,
      other.z2.im
    )

  def hasEdge(z: Complex): Boolean = z == z1 || z == z2

  def middle: Complex = (z1 + z2) / 2

  def edges: List[Complex] = List(z1, z2)

}

object Segment {
  def tupled(z: (Complex, Complex)): Segment = Segment(z._1, z._2)
}
