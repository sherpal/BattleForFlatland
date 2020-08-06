package gamelogic.physics.pathfinding
import gamelogic.entities.PolygonBody
import gamelogic.physics.Complex
import scala.Ordering.Double.TotalOrdering

import scala.annotation.tailrec

/**
  * A [[ManhattanGraph]] makes an A* algorithm by walking on a grid, allowing path to go to 8 possible positions
  * at all times.
  */
final class ManhattanGraph(gridSize: Double, maxStep: Int, isLegalPosition: Complex => Boolean, maxDistance: Double)
    extends Graph {

  /**
    * Returns the list of the 8 vertices that are around z, in such a way that the basis of the induced square is
    * parallel to the `baseDirection`. The 8 vertices are filtered to keep only those that are legal.
    * @param z current point
    * @param baseDirection Complex indicating the main direction angle. Must be of norm 1
    * @return Part of the 8 surrounding vertices that are a legal position.
    */
  private def nextPossible(z: Complex, baseDirection: Complex): List[Complex] =
    for {
      j <- List(-1, 0, 1)
      k <- List(-1, 0, 1)
      if j != 0 || k != 0
      w = z + (gridSize * Complex(j, k)) * baseDirection
      if isLegalPosition(w)
    } yield w

  /**
    * Will go through the world iteratively, with `gridSize` step length, avoiding all the `obstacles` on the way
    * and going
    * @param start starting point of the path.
    * @param end end of the path
    * @param heuristicFunction guess of the distance between two points. Usually the L2-norm
    *  @return if the shortest path exists, returns it wrapped in Some. Otherwise returns None
    */
  def a_*(start: Complex, end: Complex, heuristicFunction: (Complex, Complex) => Double): Option[List[Complex]] = {

    val baseDirection = Complex.rotation((end - start).arg)

    def h(z: Complex): Double               = heuristicFunction(z, end)
    def d(z1: Complex, z2: Complex): Double = euclideanDistance(z1, z2)

    @scala.annotation.tailrec
    def exploration(
        openSet: Map[Complex, Score],
        closedSet: Set[Complex],
        cameFromMap: Map[Complex, Complex],
        stepCount: Int
    ): Map[Complex, Complex] =
      if (openSet.isEmpty || stepCount > maxStep) Map()
      else {
        val (currentVertex, currentVertexScore) = openSet.minBy(_._2.f)

        if (currentVertex.distanceTo(end) <= maxDistance) cameFromMap
        else {
          val newOpenSet   = openSet - currentVertex
          val newClosedSet = closedSet + currentVertex

          val newElementsInOpenSet = nextPossible(currentVertex, baseDirection)
            .filterNot(closedSet.contains)
            .map(
              neighbour =>
                (
                  neighbour,
                  currentVertexScore.g + d(currentVertex, neighbour),
                  openSet.getOrElse(neighbour, defaultScore)
                )
            )
            .filter(triplet => triplet._2 < triplet._3.g)
            .map({
              case (neighbour, tentativeScoreG, _) =>
                neighbour -> Score(
                  tentativeScoreG + h(neighbour),
                  tentativeScoreG
                )
            })
            .toMap

          exploration(
            newOpenSet ++ newElementsInOpenSet,
            newClosedSet,
            cameFromMap ++ newElementsInOpenSet.keys
              .map(_ -> currentVertex)
              .toMap,
            stepCount + 1
          )
        }
      }

    val explorationResult =
      exploration(Map(start -> Score(h(start), 0)), Set(), Map(), 1)

    Option.when(explorationResult.nonEmpty)(reconstructPath(explorationResult, start, end))
  }

  /**
    * With the ManhattanGraph we explore until we go to the point, so at any time we can consider that the target
    * is on the graph.
    */
  def closestPointTo(z: Complex): Option[Complex] = Some(z)

  def addVertices(vertices: Complex*): ManhattanGraph = this
}
