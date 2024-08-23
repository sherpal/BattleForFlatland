package gamelogic

import gamelogic.physics.shape.{ConvexPolygon, Polygon, Shape}
import org.scalacheck.Gen

package object physics {

  final val angleGen = Gen.choose(0.0, 2 * math.Pi)

  def regularPolygonGen(nbrSides: Int): Gen[ConvexPolygon] =
    for {
      radius     <- Gen.choose(1.0, 1000.0)
      startAngle <- angleGen
    } yield Shape.regularPolygon(nbrSides, radius, startAngle)

  def regularPolygonWithSideNbrBetween(low: Int, high: Int): Gen[ConvexPolygon] =
    for {
      nbrSides <- Gen.choose(low, high)
      polygon  <- regularPolygonGen(nbrSides)
    } yield polygon

  val complexGen: Gen[Complex] = for {
    real <- Gen.choose[Double](-1000, 1000)
    imag <- Gen.choose[Double](-1000, 1000)
  } yield Complex(real, imag)

  def complexGenFrom(realGen: Gen[Double], imagGen: Gen[Double]): Gen[Complex] =
    for {
      real <- realGen
      imag <- imagGen
    } yield Complex(real, imag)

  val quadrilateralGen: Gen[Polygon] = for {
    inFirstQuadrant  <- complexGenFrom(Gen.choose(0.01, 1000.0), Gen.choose(0.01, 1000.0))
    inSecondQuadrant <- complexGenFrom(Gen.choose(0.01, 1000.0).map(-_), Gen.choose(0.01, 1000.0))
    inThirdQuadrant  <- complexGenFrom(Gen.choose(0.01, 1000.0).map(-_), Gen.choose(0.01, 1000.0).map(-_))
    inFourthQuadrant <- complexGenFrom(Gen.choose(0.01, 1000.0), Gen.choose(0.01, 1000.0).map(-_))
    translation      <- complexGen
    rotation         <- angleGen.map(Complex.rotation)
  } yield Polygon(
    Vector(inFirstQuadrant, inSecondQuadrant, inThirdQuadrant, inFourthQuadrant).map(_ * rotation + translation)
  )

  implicit class DoubleEnhanced(x: Double) {

    def almostEqual(y: Double): Boolean = math.abs(x - y) < 1e-6

  }

}
