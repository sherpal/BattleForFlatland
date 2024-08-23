package gamelogic.physics.shape

import gamelogic.physics.Complex

final class NonConvexPolygon(val vertices: Vector[Complex]) extends Polygon {
  lazy val triangulation: Vector[Triangle] = Shape.earClipping(vertices).toVector
}
