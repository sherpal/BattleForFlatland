package gamelogic.physics.shape

import gamelogic.physics.*
import org.scalacheck.*
import org.scalacheck.Prop.forAll

object TriangulationChecks extends Properties("TriangulationChecks") {

  property("Triangulate of quadrilateral gives 2 triangles") = forAll(quadrilateralGen) {
    quadrilateral =>
      Shape.earClipping(quadrilateral.vertices).length == 2
  }

  property("Triangulate a triangle gives 1 triangle") = forAll(polygonGen(3)) { polygon =>
    Shape.earClipping(polygon.vertices).length == 1
  }

  property("Triangulate a pentagon gives 3 triangles") =
    forAll(polygonGen(5, Shape.translatedRegularPolygon(4, 10, Complex(10, 10)))) { polygon =>
      Shape.earClipping(polygon.vertices).length == 3
    }

  property("Triangulating a clockwise polygon gives counter-clockwise triangles") =
    forAll(polygonGen(5, Shape.translatedRegularPolygon(4, 10, Complex(10, 10)))) { polygon =>
      val triangles = Shape.earClipping(polygon.vertices.reverse)
      triangles.length == 3 && triangles.forall(_.det > 0) &&
      math.abs(triangles.map(_.det / 2).sum - math.abs(Shape.signedArea(polygon.vertices))) < 1e-6
    }

  property("Generate point in triangle gives point in triangle") = forAll(for {
    triangle <- triangleGen()
    point    <- pointInTriangleGen(triangle)
  } yield (triangle, point)) { (triangle, point) =>
    triangle.contains(point.re, point.im)
  }

}
