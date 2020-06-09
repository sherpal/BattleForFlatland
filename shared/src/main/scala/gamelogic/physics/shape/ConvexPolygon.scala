package gamelogic.physics.shape

import gamelogic.physics.Complex

final class ConvexPolygon(val vertices: Vector[Complex]) extends Polygon {

  lazy val triangulation: List[Triangle] = {
    (for (j <- 1 until vertices.length - 1) yield Triangle(vertices(0), vertices(j), vertices(j + 1))).toList
  }

}
