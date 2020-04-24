package gamelogic.physics.shape

import gamelogic.physics.Complex

final class MonotonePolygon(val vertices: Vector[Complex]) extends Polygon {
  val triangulation: List[Triangle] = Shape.triangulateMonotonePolygon(vertices)
}
