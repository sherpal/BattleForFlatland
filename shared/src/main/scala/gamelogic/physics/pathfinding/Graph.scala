package gamelogic.physics.pathfinding

import gamelogic.physics.Complex

/**
  */
trait Graph {

  /** Used in a_* algorithms */
  protected def reconstructPath(
      cameFromMap: Map[Complex, Complex],
      start: Complex,
      end: Complex
  ): List[Complex] = {
    @scala.annotation.tailrec
    def withAccumulator(currentPath: List[Complex]): List[Complex] =
      if (currentPath.head == start) currentPath
      else withAccumulator(cameFromMap(currentPath.head) +: currentPath)

    withAccumulator(List(end))
  }

  def closestPointTo(z: Complex): Option[Complex]

  def addVertices(
      vertices: Complex*
  ): Graph

  /**
    * Finds the shortest path between the start and the end in the graph.
    *
    * Implements a simple A* algorithm
    *
    * @param start starting point of the path.
    * @param end end of the path
    * @param heuristicFunction guess of the distance between two points. Usually the L2-norm
    * @return if the shortest path exists, returns it wrapped in Some. Otherwise returns None
    */
  def a_*(
      start: Complex,
      end: Complex,
      heuristicFunction: (Complex, Complex) => Double
  ): Option[List[Complex]]

  /** Usual distance in the plane. */
  val euclideanDistance: (Complex, Complex) => Double = _ distanceTo _

  /** Applies the A* algorithm with the euclidean distance. See `a_*` for details. */
  def euclideanA_*(start: Complex, end: Complex): Option[List[Complex]] = a_*(start, end, euclideanDistance)

  /** Used in a_* algorithm */
  protected final case class Score(f: Double, g: Double)
  protected val defaultScore: Score = Score(Double.MaxValue, Double.MaxValue)

}
