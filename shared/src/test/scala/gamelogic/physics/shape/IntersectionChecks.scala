package gamelogic.physics.shape

import gamelogic.physics._
import org.scalacheck._
import org.scalacheck.Prop.forAll

object IntersectionChecks extends Properties("IntersectionOfSegments") {

  property("4 cross points in square intersect") = forAll(regularPolygonGen(4)) { shape =>
    val zs = shape.vertices
    Shape.intersectingSegments(
      zs(0),
      zs(2),
      zs(1),
      zs(3)
    )

  }

  property("4 consecutive points in square do not intersect") = forAll(regularPolygonGen(4)) { shape =>
    val zs = shape.vertices
    !Shape.intersectingSegments(zs(0), zs(1), zs(2), zs(3))
  }

  property("4 consecutive points in a polygon with more than 4 sides do not intersect") =
    forAll(regularPolygonWithSideNbrBetween(5, 15)) { shape =>
      val zs = shape.vertices
      !Shape.intersectingSegments(zs(0), zs(1), zs(2), zs(3))
    }

  property("Regular polygons are convex") = forAll(regularPolygonWithSideNbrBetween(3, 10)) { _.isConvex }

  property("Diagonal of quadrilateral intersect iff it is convex") = forAll(quadrilateralGen) { polygon =>
    val zs = polygon.vertices
    polygon.isConvex == Shape.intersectingSegments(zs(0), zs(2), zs(1), zs(3))
  }

}
