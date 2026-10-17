package gamelogic.physics.pathfinding

import gamelogic.entities.PolygonBody
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape

import scala.collection.mutable

/** Path finding for a disk of a given `radius` among static polygonal obstacles, using a visibility
  * graph.
  *
  * Shortest paths around polygons only ever bend at the convex corners of the obstacles. For a disk
  * of radius r, they bend "r away" from these corners. The graph is therefore built once (per
  * radius, and each time obstacles change) as follows:
  *   - for each convex corner of each obstacle, we put a waypoint slightly more than r away from
  *     the corner (on the bisector, or two waypoints for very sharp corners). Waypoints where the
  *     disk would overlap another obstacle are discarded (that happens with obstacles overlapping
  *     each other, like walls at the corners of an arena).
  *   - two waypoints are linked if the disk can slide from one to the other in a straight line.
  *
  * Querying a path then only means linking the start and the goal to the waypoints they see, and
  * running A* on a graph which has, for typical arenas, a few dozens of vertices. This is cheap
  * enough to be done at each AI frame, and since the result is the actual shortest path, it does
  * not jitter from one frame to the next (as opposed to a grid based search).
  *
  * All the geometry is done directly with the original obstacles (distance from segments to
  * obstacle edges), which means that we never need to compute the (messy) union of inflated
  * obstacles.
  *
  * Obstacles can be non convex, and can overlap. A polygon whose boundary goes around a hole (outer
  * boundary, bridge, reversed inner boundary, bridge back) is also supported, which allows to
  * create "rooms".
  *
  * @param polygons
  *   vertices of the obstacles, in world coordinates. Orientation does not matter.
  * @param radius
  *   radius of the entities which will use this graph
  * @param margin
  *   extra distance kept from obstacles, so that entities don't graze them (which, due to floating
  *   point errors, could make the game consider that they collide).
  */
final class VisibilityGraph(
    polygons: Vector[Vector[Complex]],
    val radius: Double,
    margin: Double = VisibilityGraph.defaultMargin
) extends Graph {

  /** Clearance kept along paths. Waypoints are at radius + margin, so that edges which run along an
    * obstacle are accepted.
    */
  private val pathClearance: Double = radius + margin / 2

  /** If an entity is closer than this from an obstacle, we consider it stuck inside it and first
    * bring it back to the nearest legal position.
    */
  private val stuckClearance: Double = radius / 2

  // Obstacle edges, stored flat for speed. Edges of obstacle k are in [edgeStart(k), edgeStart(k + 1)).
  private val validPolygons = polygons.filter(_.length >= 3)
  private val nbrObstacles  = validPolygons.length
  private val edgeStart     = validPolygons.scanLeft(0)(_ + _.length).toArray
  private val nbrEdges      = edgeStart.last
  private val ax            = new Array[Double](nbrEdges)
  private val ay            = new Array[Double](nbrEdges)
  private val bx            = new Array[Double](nbrEdges)
  private val by            = new Array[Double](nbrEdges)
  private val minX          = new Array[Double](nbrObstacles)
  private val minY          = new Array[Double](nbrObstacles)
  private val maxX          = new Array[Double](nbrObstacles)
  private val maxY          = new Array[Double](nbrObstacles)

  validPolygons.zipWithIndex.foreach { (polygon, k) =>
    polygon.indices.foreach { j =>
      val z1 = polygon(j)
      val z2 = polygon(if j == polygon.length - 1 then 0 else j + 1)
      val e  = edgeStart(k) + j
      ax(e) = z1.re
      ay(e) = z1.im
      bx(e) = z2.re
      by(e) = z2.im
    }
    minX(k) = polygon.iterator.map(_.re).min
    minY(k) = polygon.iterator.map(_.im).min
    maxX(k) = polygon.iterator.map(_.re).max
    maxY(k) = polygon.iterator.map(_.im).max
  }

  /** Waypoints of the graph. Every waypoint is a legal position. */
  val waypoints: Vector[Complex] = {
    val waypointDistance = radius + margin
    validPolygons
      .flatMap { polygon =>
        val n           = polygon.length
        val orientation = math.signum(Shape.signedArea(polygon))
        polygon.indices.flatMap { j =>
          val previous = polygon(if j == 0 then n - 1 else j - 1)
          val vertex   = polygon(j)
          val next     = polygon(if j == n - 1 then 0 else j + 1)
          val d1       = vertex - previous
          val d2       = next - vertex
          val isConvexCorner =
            d1.modulus2 > 1e-12 && d2.modulus2 > 1e-12 &&
              d1.crossProduct(d2) * orientation > 1e-9 * d1.modulus * d2.modulus

          if !isConvexCorner then Vector.empty
          else {
            val u1       = d1.normalized
            val u2       = d2.normalized
            val n1       = -orientation * u1.orthogonal // outward normals
            val n2       = -orientation * u2.orthogonal
            val bisector = (n1 + n2).normalized
            val cosHalf  = bisector |*| n1
            if cosHalf >= 0.6 then Vector(vertex + waypointDistance / cosHalf * bisector)
            else
              // very sharp corner, the miter would go too far: we "cut" it with two waypoints
              Vector(
                vertex + waypointDistance * (n1 + u1),
                vertex + waypointDistance * (n2 - u2)
              )
          }
        }
      }
      .filter(z => isLegalWithClearance(z.re, z.im, radius))
      .distinct
  }

  private val nbrWaypoints = waypoints.length
  private val waypointsX   = waypoints.map(_.re).toArray
  private val waypointsY   = waypoints.map(_.im).toArray

  /** For each waypoint, the indices of the waypoints it is linked to. */
  private val adjacency: Array[Array[Int]] = {
    val neighbours = Array.fill(nbrWaypoints)(mutable.ArrayBuilder.make[Int])
    for {
      i <- 0 until nbrWaypoints
      j <- i + 1 until nbrWaypoints
      if segmentIsClear(waypointsX(i), waypointsY(i), waypointsX(j), waypointsY(j), pathClearance)
    } {
      neighbours(i) += j
      neighbours(j) += i
    }
    neighbours.map(_.result())
  }

  /** All the links of the graph (each link appearing once). Mostly useful for debugging. */
  lazy val links: Vector[(Complex, Complex)] =
    (for {
      i <- 0 until nbrWaypoints
      j <- adjacency(i)
      if i < j
    } yield (waypoints(i), waypoints(j))).toVector

  def isLegal(z: Complex): Boolean = isLegalWithClearance(z.re, z.im, radius)

  def isSegmentClear(from: Complex, to: Complex, clearance: Double): Boolean =
    segmentIsClear(from.re, from.im, to.re, to.im, clearance)

  /** Distance from z to the closest obstacle (0 if z is inside an obstacle). */
  def clearanceAt(z: Complex): Double = {
    var best = Double.PositiveInfinity
    var k    = 0
    while k < nbrObstacles do {
      if insideObstacle(k, z.re, z.im) then return 0.0
      var e = edgeStart(k)
      while e < edgeStart(k + 1) do {
        val d2 = VisibilityGraph.pointSegmentDistance2(z.re, z.im, ax(e), ay(e), bx(e), by(e))
        if d2 < best then best = d2
        e += 1
      }
      k += 1
    }
    math.sqrt(best)
  }

  def nearestLegalPosition(z: Complex): Complex = {
    val targetClearance = radius + margin

    @scala.annotation.tailrec
    def pushOut(z: Complex, remainingTrials: Int): Option[Complex] =
      if clearanceAt(z) >= pathClearance then Some(z)
      else if remainingTrials == 0 then None
      else {
        val (closestPoint, edgeIndex) = closestObstaclePoint(z)
        val inside                    = (0 until nbrObstacles).exists(insideObstacle(_, z.re, z.im))
        val away                      = if inside then closestPoint - z else z - closestPoint
        val direction =
          if away.modulus2 > 1e-12 then away.normalized
          else
            // z is exactly on the edge, we take any normal to it, and hope for the best
            Complex(
              bx(edgeIndex) - ax(edgeIndex),
              by(edgeIndex) - ay(edgeIndex)
            ).orthogonal.normalized
        pushOut(closestPoint + targetClearance * direction, remainingTrials - 1)
      }

    pushOut(z, 6).getOrElse(waypoints.minByOption(_.distanceTo(z)).getOrElse(z))
  }

  def findPath(start: Complex, goal: Complex): Option[Vector[Complex]] = {
    val startClearance = clearanceAt(start)
    if startClearance < stuckClearance then {
      val escape = nearestLegalPosition(start)
      if escape == start then None
      else
        findPathFromLegalStart(escape, math.min(pathClearance, clearanceAt(escape)), goal)
          .map(start +: _)
    } else findPathFromLegalStart(start, math.min(pathClearance, startClearance * 0.999), goal)
  }

  /** A* search, where the start (and its clearance) is assumed to be suitable. */
  private def findPathFromLegalStart(
      start: Complex,
      startClearance: Double,
      wantedGoal: Complex
  ): Option[Vector[Complex]] = {
    val goal =
      if clearanceAt(wantedGoal) >= pathClearance then wantedGoal
      else nearestLegalPosition(wantedGoal)

    if segmentIsClear(start.re, start.im, goal.re, goal.im, startClearance) then
      Some(Vector(start, goal))
    else {
      val startIndex = nbrWaypoints
      val goalIndex  = nbrWaypoints + 1

      inline def x(index: Int): Double =
        if index < nbrWaypoints then waypointsX(index)
        else if index == startIndex then start.re
        else goal.re
      inline def y(index: Int): Double =
        if index < nbrWaypoints then waypointsY(index)
        else if index == startIndex then start.im
        else goal.im
      inline def distance(i: Int, j: Int): Double = math.hypot(x(i) - x(j), y(i) - y(j))
      inline def heuristic(i: Int): Double        = distance(i, goalIndex)

      // 0: not computed yet, 1: sees the goal, 2: does not see the goal
      val seesGoal = new Array[Byte](nbrWaypoints)
      def waypointSeesGoal(i: Int): Boolean = {
        if seesGoal(i) == 0 then
          seesGoal(i) =
            if segmentIsClear(waypointsX(i), waypointsY(i), goal.re, goal.im, pathClearance) then 1
            else 2
        seesGoal(i) == 1
      }

      val bestCost = Array.fill(nbrWaypoints + 2)(Double.PositiveInfinity)
      val cameFrom = Array.fill(nbrWaypoints + 2)(-1)
      val closed   = new Array[Boolean](nbrWaypoints + 2)
      // ordered by smallest estimated total cost first
      val openSet = mutable.PriorityQueue.empty[(Double, Int)](using
        Ordering.by[(Double, Int), Double](-_._1)(using Ordering.Double.TotalOrdering)
      )

      def relax(from: Int, to: Int): Unit = {
        val tentativeCost = bestCost(from) + distance(from, to)
        if tentativeCost < bestCost(to) then {
          bestCost(to) = tentativeCost
          cameFrom(to) = from
          openSet.enqueue((tentativeCost + heuristic(to), to))
        }
      }

      bestCost(startIndex) = 0.0
      closed(startIndex) = true
      (0 until nbrWaypoints).foreach { i =>
        if segmentIsClear(start.re, start.im, waypointsX(i), waypointsY(i), startClearance) then
          relax(startIndex, i)
      }

      var found = false
      while !found && openSet.nonEmpty do {
        val (_, current) = openSet.dequeue()
        if current == goalIndex then found = true
        else if !closed(current) then {
          closed(current) = true
          adjacency(current).foreach(neighbour =>
            if !closed(neighbour) then relax(current, neighbour)
          )
          if waypointSeesGoal(current) then relax(current, goalIndex)
        }
      }

      Option.when(found) {
        @scala.annotation.tailrec
        def reconstruct(index: Int, acc: List[Complex]): List[Complex] =
          if index == startIndex then start :: acc
          else
            reconstruct(
              cameFrom(index),
              (if index == goalIndex then goal else waypoints(index)) :: acc
            )

        reconstruct(goalIndex, Nil).toVector
      }
    }
  }

  private def closestObstaclePoint(z: Complex): (Complex, Int) = {
    var bestDistance = Double.PositiveInfinity
    var bestEdge     = 0
    var e            = 0
    while e < nbrEdges do {
      val d2 = VisibilityGraph.pointSegmentDistance2(z.re, z.im, ax(e), ay(e), bx(e), by(e))
      if d2 < bestDistance then {
        bestDistance = d2
        bestEdge = e
      }
      e += 1
    }
    val a  = Complex(ax(bestEdge), ay(bestEdge))
    val b  = Complex(bx(bestEdge), by(bestEdge))
    val ab = b - a
    val t =
      if ab.modulus2 == 0 then 0.0 else math.max(0.0, math.min(1.0, ((z - a) |*| ab) / ab.modulus2))
    (a + t * ab, bestEdge)
  }

  /** Even-odd rule, which correctly handles polygons with a hole joined by a bridge. */
  private def insideObstacle(k: Int, x: Double, y: Double): Boolean = {
    var inside = false
    if x >= minX(k) && x <= maxX(k) && y >= minY(k) && y <= maxY(k) then {
      var e = edgeStart(k)
      while e < edgeStart(k + 1) do {
        val y1 = ay(e)
        val y2 = by(e)
        if (y1 > y) != (y2 > y) then {
          val xCross = ax(e) + (y - y1) / (y2 - y1) * (bx(e) - ax(e))
          if x < xCross then inside = !inside
        }
        e += 1
      }
    }
    inside
  }

  private def isLegalWithClearance(x: Double, y: Double, clearance: Double): Boolean = {
    val clearance2 = clearance * clearance
    var k          = 0
    var legal      = true
    while legal && k < nbrObstacles do {
      val nearby = x >= minX(k) - clearance && x <= maxX(k) + clearance &&
        y >= minY(k) - clearance && y <= maxY(k) + clearance
      if nearby then {
        if insideObstacle(k, x, y) then legal = false
        else {
          var e = edgeStart(k)
          while legal && e < edgeStart(k + 1) do {
            if VisibilityGraph.pointSegmentDistance2(x, y, ax(e), ay(e), bx(e), by(e)) < clearance2
            then legal = false
            e += 1
          }
        }
      }
      k += 1
    }
    legal
  }

  /** Whether a disk of radius `clearance` can go from (x1, y1) to (x2, y2) without overlapping any
    * obstacle.
    */
  private def segmentIsClear(
      x1: Double,
      y1: Double,
      x2: Double,
      y2: Double,
      clearance: Double
  ): Boolean = {
    val clearance2  = clearance * clearance
    val segmentMinX = math.min(x1, x2) - clearance
    val segmentMaxX = math.max(x1, x2) + clearance
    val segmentMinY = math.min(y1, y2) - clearance
    val segmentMaxY = math.max(y1, y2) + clearance
    var k           = 0
    var clear       = true
    while clear && k < nbrObstacles do {
      if segmentMinX <= maxX(k) && segmentMaxX >= minX(k) && segmentMinY <= maxY(
          k
        ) && segmentMaxY >= minY(k)
      then {
        // if the segment does not cross the boundary, it is either fully inside or fully outside
        if insideObstacle(k, x1, y1) || insideObstacle(k, (x1 + x2) / 2, (y1 + y2) / 2) then
          clear = false
        else {
          var e = edgeStart(k)
          while clear && e < edgeStart(k + 1) do {
            val distance2 =
              VisibilityGraph.segmentSegmentDistance2(x1, y1, x2, y2, ax(e), ay(e), bx(e), by(e))
            // with a 0 clearance, crossing the boundary still has to be forbidden
            if distance2 < clearance2 || (distance2 == 0 && clearance2 == 0) then clear = false
            e += 1
          }
        }
      }
      k += 1
    }
    clear
  }

}

object VisibilityGraph {

  val defaultMargin: Double = 3.0

  def apply(obstacles: Iterable[PolygonBody], radius: Double): VisibilityGraph =
    new VisibilityGraph(
      obstacles.iterator
        .map(body => body.shape.translateAndRotationVertices(body.pos, body.rotation))
        .toVector,
      radius
    )

  /** Squared distance from (px, py) to the segment [(x1, y1), (x2, y2)]. */
  def pointSegmentDistance2(
      px: Double,
      py: Double,
      x1: Double,
      y1: Double,
      x2: Double,
      y2: Double
  ): Double = {
    val dx   = x2 - x1
    val dy   = y2 - y1
    val len2 = dx * dx + dy * dy
    val t =
      if len2 == 0 then 0.0
      else math.max(0.0, math.min(1.0, ((px - x1) * dx + (py - y1) * dy) / len2))
    val cx = x1 + t * dx - px
    val cy = y1 + t * dy - py
    cx * cx + cy * cy
  }

  /** Whether the two segments cross each other "properly" (touching does not count). */
  def segmentsCross(
      x11: Double,
      y11: Double,
      x12: Double,
      y12: Double,
      x21: Double,
      y21: Double,
      x22: Double,
      y22: Double
  ): Boolean = {
    inline def cross(
        ox: Double,
        oy: Double,
        px: Double,
        py: Double,
        qx: Double,
        qy: Double
    ): Double =
      (px - ox) * (qy - oy) - (py - oy) * (qx - ox)
    val d1 = cross(x21, y21, x22, y22, x11, y11)
    val d2 = cross(x21, y21, x22, y22, x12, y12)
    val d3 = cross(x11, y11, x12, y12, x21, y21)
    val d4 = cross(x11, y11, x12, y12, x22, y22)
    ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
  }

  /** Squared distance between the two segments. */
  def segmentSegmentDistance2(
      x11: Double,
      y11: Double,
      x12: Double,
      y12: Double,
      x21: Double,
      y21: Double,
      x22: Double,
      y22: Double
  ): Double =
    if segmentsCross(x11, y11, x12, y12, x21, y21, x22, y22) then 0.0
    else
      math.min(
        math.min(
          pointSegmentDistance2(x11, y11, x21, y21, x22, y22),
          pointSegmentDistance2(x12, y12, x21, y21, x22, y22)
        ),
        math.min(
          pointSegmentDistance2(x21, y21, x11, y11, x12, y12),
          pointSegmentDistance2(x22, y22, x11, y11, x12, y12)
        )
      )

}
