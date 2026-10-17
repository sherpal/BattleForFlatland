package gamelogic.physics.pathfinding

import gamelogic.entities.staticstuff.Obstacle
import gamelogic.physics.Complex
import gamelogic.physics.Complex.i
import gamelogic.physics.shape.Shape
import org.scalacheck.*
import org.scalacheck.Prop.forAll

object VisibilityGraphChecks extends Properties("VisibilityGraph") {

  val radius = 35.0

  def rectangle(center: Complex, width: Double, height: Double): Vector[Complex] =
    Vector(
      center + Complex(-width / 2, -height / 2),
      center + Complex(width / 2, -height / 2),
      center + Complex(width / 2, height / 2),
      center + Complex(-width / 2, height / 2)
    )

  /** Same walls as `Obstacle.squareGameArea`: they overlap at the corners. */
  def arenaWalls(size: Double): Vector[Vector[Complex]] =
    Vector[(Complex, (Complex, Complex))](
      (size, (-i * size, i * size)),
      (-size, (-i * size, i * size)),
      (-i * size, (-size, size)),
      (i * size, (-size, size))
    ).map { case (position, (z1, z2)) =>
      Obstacle.segmentObstacleVertices(z1, z2, 10).map(_ + position)
    }

  /** A U opened towards the left, a typical trap for greedy algorithms. Not convex. */
  val uShape: Vector[Complex] = Vector(
    Complex(-100, -200),
    Complex(100, -200),
    Complex(100, 200),
    Complex(-100, 200),
    Complex(-100, 160),
    Complex(60, 160),
    Complex(60, -160),
    Complex(-100, -160)
  )

  /** Hexagonal room built as one polygon with a hole (outer boundary, bridge, reversed inner
    * boundary).
    */
  val room: Vector[Complex] = {
    val outer = Shape.regularPolygon(6, 415).vertices
    val inner = Shape.regularPolygon(6, 400).vertices
    (outer :+ outer.head) ++ (inner :+ inner.head).reverse
  }

  val arenaWithU: VisibilityGraph = new VisibilityGraph(arenaWalls(500) :+ uShape, radius)

  def pathIsValid(graph: VisibilityGraph, path: Vector[Complex]): Boolean =
    path.tail.forall(graph.isLegal) &&
      path.zip(path.tail).forall((z, w) => graph.isSegmentClear(z, w, graph.radius))

  def pathLength(path: Vector[Complex]): Double =
    path.zip(path.tail).map(_.distanceTo(_)).sum

  /** Moves an entity at 30 FPS by heading to the first waypoint of a freshly computed path at each
    * frame, as the AIs do. Returns the positions visited.
    */
  def simulate(
      graph: VisibilityGraph,
      start: Complex,
      goal: Complex,
      speed: Double,
      maxFrames: Int
  ): Vector[Complex] = {
    val frameDuration = 1.0 / 30
    Iterator
      .iterate(Option(start)) {
        case None => None
        case Some(position) =>
          graph
            .findPath(position, goal)
            .flatMap(_.find(_.distanceTo(position) > 1.0))
            .map { waypoint =>
              val step = speed * frameDuration
              if waypoint.distanceTo(position) <= step then waypoint
              else position + step * (waypoint - position).normalized
            }
      }
      .take(maxFrames)
      .takeWhile(_.isDefined)
      .flatten
      .toVector
  }

  property("goes straight when nothing is in the way") = {
    val graph = new VisibilityGraph(Vector.empty, radius)
    graph.findPath(0, Complex(100, 100)) == Some(Vector(Complex.zero, Complex(100, 100)))
  }

  property("waypoints are legal, and walls overlapping at corners don't create bad ones") = {
    val graph = new VisibilityGraph(arenaWalls(500), radius)
    graph.waypoints.forall(graph.isLegal) &&
    // only the four inner corners of the arena are convex corners reachable by the entity, and
    // they are reflex from the free space point of view, so no waypoint should be inside the arena
    graph.waypoints.forall(z => math.abs(z.re) > 500 || math.abs(z.im) > 500)
  }

  property("goes around a non convex obstacle when starting inside it") = {
    val start = Complex(0, 0)   // inside the cup of the U
    val goal  = Complex(300, 0) // behind the U
    arenaWithU.findPath(start, goal) match {
      case Some(path) =>
        pathIsValid(arenaWithU, path) &&
        path.length > 2 &&
        pathLength(path) > 2 * 160 // must go out of the cup first
      case None => false
    }
  }

  property("ends at a legal position close to the goal when the goal is not legal") = {
    val goal = Complex(80, 0) // inside the right side of the U
    arenaWithU.findPath(Complex(-300, 0), goal).exists { path =>
      pathIsValid(arenaWithU, path) && path.last.distanceTo(goal) < 2 * radius + 20
    }
  }

  property("returns None when the goal is enclosed") = {
    val graph = new VisibilityGraph(arenaWalls(500), radius)
    val boxed = new VisibilityGraph(
      arenaWalls(500) ++ Vector(
        rectangle(Complex(0, 100), 210, 10),
        rectangle(Complex(0, -100), 210, 10),
        rectangle(Complex(100, 0), 10, 210),
        rectangle(Complex(-100, 0), 10, 210)
      ),
      radius
    )
    graph.findPath(Complex(-300, -300), 0).isDefined &&
    boxed.findPath(Complex(-300, -300), 0).isEmpty
  }

  property("works in a room built with a hole, with pillars") = {
    val pillars = (0 until 6).toVector.map { j =>
      val center =
        240 * Complex.rotation(gamelogic.entities.WithPosition.Angle.fromDouble(j * math.Pi / 3))
      Shape.regularPolygon(3, 45).vertices.map(_ + center)
    }
    val graph = new VisibilityGraph(room +: pillars, 15)
    val start = Complex(-300, 0)
    val goal  = Complex(300, 0)
    graph.findPath(start, goal).exists(pathIsValid(graph, _)) &&
    graph
      .findPath(start, Complex(1000, 0))
      .forall(_.last.distanceTo(0) < 400) // outside -> projected inside
  }

  property("an entity re-planning at each frame reaches the goal without colliding") = {
    val start     = Complex(0, 0)
    val goal      = Complex(300, 0)
    val positions = simulate(arenaWithU, start, goal, speed = 300, maxFrames = 300)
    positions.last.distanceTo(goal) < 1.0 &&
    positions.forall(z => arenaWithU.clearanceAt(z) >= radius) &&
    positions.zip(positions.tail).forall((z, w) => arenaWithU.isSegmentClear(z, w, radius))
  }

  property("random paths in a cluttered arena are valid") = {
    val pillars = Vector(
      rectangle(Complex(-200, -200), 80, 80),
      rectangle(Complex(200, 200), 80, 80),
      Shape.regularPolygon(3, 60).vertices.map(_ + Complex(-200, 200)),
      Shape.regularPolygon(5, 60).vertices.map(_ + Complex(200, -200))
    )
    val graph = new VisibilityGraph(arenaWalls(500) ++ pillars :+ uShape, 15)
    val positionGen =
      Gen.zip(Gen.choose(-480.0, 480.0), Gen.choose(-480.0, 480.0)).map(Complex(_, _))

    forAll(positionGen.suchThat(graph.isLegal), positionGen) { (start, goal) =>
      graph.findPath(start, goal).forall { path =>
        path.head == start && pathIsValid(graph, path)
      }
    }
  }

  property("queries are fast enough to be run at each frame by many entities") = {
    val pillars = (0 until 10).toVector.map(j =>
      Shape
        .regularPolygon(4 + j % 3, 40)
        .vertices
        .map(_ + Complex(-400 + 80 * j, 200 * math.sin(j)))
    )
    val graph = new VisibilityGraph(arenaWalls(500) ++ pillars :+ uShape, 35)
    val rng   = new scala.util.Random(42)
    val queries = Vector.fill(2000)(
      (Complex(rng.between(-480.0, 480.0), rng.between(-480.0, 480.0)), Complex(300, 0))
    )

    queries.take(200).foreach(graph.findPath) // warm up
    val startTime = System.nanoTime()
    queries.foreach(graph.findPath)
    val averageMicros = (System.nanoTime() - startTime) / 1000.0 / queries.length
    println(
      s"[VisibilityGraph] ${graph.waypoints.length} waypoints, ${graph.links.length} links, $averageMicros µs/query"
    )
    averageMicros < 1000 // 1ms per query is already a lot of slack
  }

}
