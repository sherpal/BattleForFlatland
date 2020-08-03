package gamelogic.physics.shape

import gamelogic.physics.Complex
import org.scalacheck._
import org.scalacheck.Prop.forAll

object IntersectionChecks extends Properties("IntersectionOfSegments") {

  private def regularPolygonGen(nbrSides: Int) =
    for {
      radius <- Gen.choose(1.0, 1000.0)
      startAngle <- Gen.choose(0.0, 2 * math.Pi)
    } yield Shape.regularPolygon(nbrSides, radius, startAngle)

  private def regularPolygonWithSideNbrBetween(low: Int, high: Int) =
    for {
      nbrSides <- Gen.choose(low, high)
      polygon <- regularPolygonGen(nbrSides)
    } yield polygon

  private val complexGen = for {
    real <- Gen.choose[Double](-1000, 1000)
    imag <- Gen.choose[Double](-1000, 1000)
  } yield Complex(real, imag)

  private def complexGenFrom(realGen: Gen[Double], imagGen: Gen[Double]): Gen[Complex] =
    for {
      real <- realGen
      imag <- imagGen
    } yield Complex(real, imag)

  private val quadrilateralGen = for {
    inFirstQuadrant <- complexGenFrom(Gen.choose(0.01, 1000), Gen.choose(0.01, 1000))
    inSecondQuadrant <- complexGenFrom(Gen.choose(0.01, 1000).map(-_), Gen.choose(0.01, 1000))
    inThirdQuadrant <- complexGenFrom(Gen.choose(0.01, 1000).map(-_), Gen.choose(0.01, 1000).map(-_))
    inFourthQuadrant <- complexGenFrom(Gen.choose(0.01, 1000), Gen.choose(0.01, 1000).map(-_))
    translation <- complexGen
    rotation <- Gen.choose(0.0, 2 * math.Pi).map(Complex.rotation)
  } yield Polygon(
    Vector(inFirstQuadrant, inSecondQuadrant, inThirdQuadrant, inFourthQuadrant).map(_ * rotation + translation)
  )

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
