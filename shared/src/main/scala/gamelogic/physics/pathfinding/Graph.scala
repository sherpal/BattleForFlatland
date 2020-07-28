package gamelogic.physics.pathfinding

import gamelogic.physics.Complex
import gamelogic.physics.quadtree.ShapeQT
import gamelogic.physics.shape.{Segment, Shape}

import scala.Ordering.Double.TotalOrdering

/**
  * A Graph is a set of vertices that are connected together.
  * The connection information is stored in the `neighboursMap` which maps a vertex to all its neighbours.
  * @param vertices complex numbers in the graph
  * @param neighboursMap describes the relationship between vertices.
  */
final class Graph(
    val vertices: Vector[Complex],
    val neighboursMap: Map[Complex, List[Complex]],
    quadTree: ShapeQT,
    inflatedEdges: Iterable[Segment]
) {

  lazy val allEdges: List[(Complex, Complex)] =
    neighboursMap
      .flatMap { case (z, ws) => ws.map((z, _)) }
      .toList
      .distinctBy { case (z, w) => if (Complex.polarOrder(z, w) <= 0) (z, w) else (w, z) }

  def closestPointTo(z: Complex): Option[Complex] =
    vertices
      .find(_ == z) // this is unlikely to happen, should we keep this check?
      .orElse(allEdges.map(Shape.closestToSegment(_, z)).minByOption(z.distanceTo))

  def addVertex(
      vertex: Complex
  ): Graph = {
    val newNeighboursMap =
      if (neighboursMap.isDefinedAt(vertex)) neighboursMap
      else {
        val possibleSegments = inflatedEdges.filterNot(_.hasEdge(vertex))
        val connectedTo = (for {
          v <- neighboursMap.keys
          if !possibleSegments
            .filterNot(_.hasEdge(v))
            .exists(segment => Shape.intersectingSegments(vertex, v, segment.z1, segment.z2))
          if !quadTree.contains((vertex + v) / 2)
        } yield v).toList

        val finalConnectedTo =
          if (connectedTo.isEmpty && neighboursMap.nonEmpty)
            List(neighboursMap.keys.minBy(z => (vertex - z).modulus2))
          else connectedTo

        (neighboursMap ++ finalConnectedTo.map(v => v -> (vertex +: neighboursMap(v)))) + (vertex -> finalConnectedTo)
      }

    new Graph(vertices :+ vertex, newNeighboursMap, quadTree, inflatedEdges)
  }

  def addVertices(
      vertices: Complex*
  ): Graph = vertices.foldLeft(this)(_.addVertex(_))

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
  ): Option[List[Complex]] = {

    def reconstructPath(cameFromMap: Map[Complex, Complex]): List[Complex] = {
      @scala.annotation.tailrec
      def withAccumulator(currentPath: List[Complex]): List[Complex] =
        if (currentPath.head == start) currentPath
        else withAccumulator(cameFromMap(currentPath.head) +: currentPath)

      withAccumulator(List(end))
    }

    def h(z: Complex): Double               = heuristicFunction(z, end)
    def d(z1: Complex, z2: Complex): Double = euclideanDistance(z1, z2)

    final case class Score(f: Double, g: Double)
    val defaultScore = Score(Double.MaxValue, Double.MaxValue)

    @scala.annotation.tailrec
    def exploration(
        openSet: Map[Complex, Score],
        closedSet: Set[Complex],
        cameFromMap: Map[Complex, Complex]
    ): Map[Complex, Complex] =
      if (openSet.isEmpty) Map()
      else {
        val (currentVertex, currentVertexScore) = openSet.minBy(_._2.f)

        if (currentVertex == end) cameFromMap
        else {
          val newOpenSet   = openSet - currentVertex
          val newClosedSet = closedSet + currentVertex

          val newElementsInOpenSet = neighboursMap(currentVertex)
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
              .toMap
          )
        }
      }

    val explorationResult =
      exploration(Map(start -> Score(h(start), 0)), Set(), Map())

    if (explorationResult.isEmpty) None
    else Some(reconstructPath(explorationResult))

  }

  /** Usual distance in the plane. */
  val euclideanDistance: (Complex, Complex) => Double = _ distanceTo _

  /** Applies the A* algorithm with the euclidean distance. See `a_*` for details. */
  def euclideanA_*(start: Complex, end: Complex): Option[List[Complex]] = a_*(start, end, euclideanDistance)

}

object Graph {

  implicit class Vertex(z: Complex) {
    def neighbours(
        implicit neighboursMap: Map[Complex, List[Complex]]
    ): List[Complex] = neighboursMap(z)
  }

}
