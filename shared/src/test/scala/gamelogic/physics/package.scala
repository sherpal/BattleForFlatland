package gamelogic

import gamelogic.physics.shape.{ConvexPolygon, Polygon, Shape}
import org.scalacheck.Gen
import gamelogic.physics.shape.Triangle

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

  def triangleGen(area: Polygon = Shape.regularPolygon(4, 100)): Gen[Triangle] = for {
    z1 <- pointInPolygonGen(area)
    z2 <- pointInPolygonGen(area)
    z3 <- pointInPolygonGen(area)
    _ = if !(z1 != z2 && z2 != z3 && z3 != z1) then println(s"oopsies, ($z1, $z2, $z3)")
    if z1 != z2 && z2 != z3 && z3 != z1
  } yield if (z2 - z1).crossProduct(z3 - z2) > 0 then Triangle(z1, z2, z3) else Triangle(z1, z3, z2)

  val quadrilateralGen: Gen[Polygon] = for {
    inFirstQuadrant  <- complexGenFrom(Gen.choose(0.01, 1000.0), Gen.choose(0.01, 1000.0))
    inSecondQuadrant <- complexGenFrom(Gen.choose(0.01, 1000.0).map(-_), Gen.choose(0.01, 1000.0))
    inThirdQuadrant <- complexGenFrom(
      Gen.choose(0.01, 1000.0).map(-_),
      Gen.choose(0.01, 1000.0).map(-_)
    )
    inFourthQuadrant <- complexGenFrom(Gen.choose(0.01, 1000.0), Gen.choose(0.01, 1000.0).map(-_))
    translation      <- complexGen
    rotation         <- angleGen.map(Complex.rotation)
  } yield Polygon(
    Vector(inFirstQuadrant, inSecondQuadrant, inThirdQuadrant, inFourthQuadrant)
      .map(_ * rotation + translation)
  )

  def pointInTriangleGen(triangle: Triangle) = for {
    x <- Gen.choose(0.0, 1.0)
    y <- Gen.choose(0.0, 1.0 - x)
    firstEdge  = triangle.vertices(1) - triangle.vertices(0)
    secondEdge = triangle.vertices(2) - triangle.vertices(0)
  } yield triangle.vertices(0) + x * firstEdge + y * secondEdge

  def pointInPolygonGen(polygon: Polygon) = for {
    triangle   <- Gen.oneOf(polygon.triangulation)
    inTriangle <- pointInTriangleGen(triangle)
  } yield inTriangle

  // todo: fix me
  def expandPolygonGen(polygon: Polygon, area: Polygon) = for {
    vertexToAdd <- pointInPolygonGen(area)
    if !polygon.vertices.contains(vertexToAdd)
    polygonEdges = polygon.vertices.zip(polygon.vertices.tail :+ polygon.vertices.head)
    closestEdge = polygonEdges.zipWithIndex
      .minBy((segment, _) => (Shape.closestToSegment(segment, vertexToAdd) - vertexToAdd).modulus2)
    (edge, index) = closestEdge
    newTriangle   = Triangle(edge._1, vertexToAdd, edge._2)
    if math.abs(newTriangle.det) > 0.02
    vertices = polygon.vertices
      .patch(index, Vector(polygon.vertices(index), vertexToAdd), 1)
    attempt = Polygon(vertices)
    edges   = attempt.edges
    selfIntersection = (for {
      j <- edges.indices
      k <- edges.indices
      if k > j + 1 && (j != 0 || k != edges.length - 1)
    } yield (edges(j), edges(k))).exists((s1, s2) => s1.intersectionPoint(s2).isDefined)
    if !selfIntersection
  } yield attempt

  def polygonGen(numberOfSides: Int, area: Polygon = Shape.regularPolygon(4, 10)): Gen[Polygon] =
    if numberOfSides < 3 then
      throw IllegalArgumentException(s"At least three sides are required, got $numberOfSides")
    else if numberOfSides == 3 then triangleGen(area).map(triangle => Polygon(triangle.vertices))
    else polygonGen(numberOfSides - 1, area).flatMap(expandPolygonGen(_, area))

  implicit class DoubleEnhanced(x: Double) {

    def almostEqual(y: Double): Boolean = math.abs(x - y) < 1e-6

  }

}
