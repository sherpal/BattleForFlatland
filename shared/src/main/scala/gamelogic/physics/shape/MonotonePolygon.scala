package gamelogic.physics.shape

import gamelogic.physics.Complex

final class MonotonePolygon(val vertices: Vector[Complex]) extends Polygon {
  lazy val triangulation: Vector[Triangle] = Shape.triangulateMonotonePolygon(vertices)
}
