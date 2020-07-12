package gamelogic.physics.pathfinding

import gamelogic.physics.Complex
import gamelogic.physics.quadtree.ShapeQT
import gamelogic.physics.shape.{NonConvexPolygon, Segment, Shape}

/**
  * Constructs a graph for the mobs to live in, avoiding obstacles in the way.
  *
  * The algorithm to build the graph is quite naive but I believe fast enough for the purpose of the games
  * I want to implement.
  *
  * - We "inflate" all the obstacles to get the maximum area where a mob of a given radius can live in
  * - For each pair of these newly created obstacles, we keep only those that do not cross another obstacle.
  * That's it!
  */
object AIWanderGraph {

  def addVertices(
      vertices: List[Complex],
      neighboursMap: Map[Complex, List[Complex]],
      quadTree: ShapeQT,
      inflatedEdges: Iterable[Segment],
      forceInclusion: Boolean = false
  ): Map[Complex, List[Complex]] =
    vertices.foldLeft(neighboursMap)({
      case (map, vertex) => addVertex(vertex, map, quadTree, inflatedEdges, forceInclusion)
    })

  def addVertex(
      vertex: Complex,
      neighboursMap: Map[Complex, List[Complex]],
      quadTree: ShapeQT,
      inflatedEdges: Iterable[Segment],
      forceInclusion: Boolean = false
  ): Map[Complex, List[Complex]] =
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
        if (connectedTo.isEmpty && forceInclusion && neighboursMap.nonEmpty)
          List(neighboursMap.keys.minBy(z => (vertex - z).modulus2))
        else connectedTo

      (neighboursMap ++ finalConnectedTo.map(v => v -> (vertex +: neighboursMap(v)))) + (vertex -> finalConnectedTo)
    }

  def apply(quadTree: ShapeQT, radius: Double): (Graph, List[Segment]) = {
    val obstacles = quadTree.shapes
      .map(obstacle => obstacle.shape.translateAndRotationVertices(obstacle.pos, obstacle.rotation))
      .map(new NonConvexPolygon(_))

    val verticesPerObstacles = obstacles.map(_.inflateWithoutPolygon(radius))

    val inflatedEdges = obstacles
      .map(_.inflateWithoutPolygon(radius * 0.9))
      .flatMap(vertices => vertices.zip(vertices.tail :+ vertices(0)).map(Segment.tupled))

    val allVertices = verticesPerObstacles.flatten.toVector

    val neighboursMapOneWay: Map[Complex, List[Complex]] =
      allVertices.indices
        .map(idx => {
          val v1               = allVertices(idx)
          val possibleSegments = inflatedEdges.filterNot(_.hasEdge(v1))
          val connectedTo: List[Complex] = {
            for {
              idx2 <- idx + 1 until allVertices.length
              v2 = allVertices(idx2)
              if !possibleSegments
                .filterNot(_.hasEdge(v2))
                .exists(segment => Shape.intersectingSegments(v1, v2, segment.z1, segment.z2))
              if !quadTree.contains((v1 + v2) / 2)
            } yield v2
          }.toList
          v1 -> connectedTo
        })
        .toMap

    val neighboursMap = neighboursMapOneWay ++ neighboursMapOneWay.toList
      .flatMap {
        case (z, zs) => zs.map(_ -> z)
      }
      .groupBy(_._1)
      .map { case (key, value) => key -> value.map(_._2) }

    (new Graph(allVertices, neighboursMap), inflatedEdges)
  }

}
