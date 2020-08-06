package gamelogic.physics.shape

import gamelogic.physics.Complex

/**
  * A convex polygon is extremely easy to triangulate, and it can be done in O(n).
  * Whenever possible, you should use the [[gamelogic.physics.shape.ConvexPolygon]] instead of a simple
  * [[gamelogic.physics.shape.NonConvexPolygon]] since the latter will need ear clipping algorithm, which
  * is O(n ** 2).
  * @param vertices positively oriented vector of vertices polygons.
  */
final class ConvexPolygon(val vertices: Vector[Complex]) extends Polygon {

  lazy val triangulation: List[Triangle] = {
    (for (j <- 1 until vertices.length - 1) yield Triangle(vertices(0), vertices(j), vertices(j + 1))).toList
  }

  override def toString: String = vertices.mkString("ConvexPolygon(", ", ", ")")

}
