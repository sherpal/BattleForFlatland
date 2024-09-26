package application.ai

import scala.collection.mutable
import gamelogic.physics.pathfinding.{Graph, ManhattanGraph}
import gamelogic.gamestate.gameactions.*
import gamelogic.physics.quadtree.ShapeQT
import gamelogic.physics.shape.Circle
import gamelogic.entities.PolygonBody
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.GameAction

class GraphManager(initialRadiuses: Iterable[Int]) {

  private val pathFindersInfo: mutable.Map[Int, PathFinderInfo] = mutable.Map.empty

  def graphs: Double => Option[Graph] = (r: Double) =>
    val approxRadius = math.round(r).toInt
    pathFindersInfo.get(approxRadius).map(_.graph)

  case class PathFinderInfo(shapeQT: ShapeQT, entityRadius: Int) {
    def addObstacles(obstacles: Iterable[PolygonBody]): PathFinderInfo =
      copy(shapeQT = shapeQT ++ obstacles.toVector)

    def removeObstaclesByIds(ids: Vector[Entity.Id]): PathFinderInfo =
      copy(shapeQT = shapeQT -- ids)

    lazy val graph: Graph =
      ManhattanGraph(
        10,
        100,
        pos => !shapeQT.collides(Circle(entityRadius), pos, 0),
        entityRadius + 5
      )

  }

  private def emptyPathFinderInfo(radius: Int) = PathFinderInfo(ShapeQT.empty, radius)

  def graphsForRadius(radius: Iterable[Int], gameState: GameState): Unit = {
    val missingRadius = radius.toSet -- pathFindersInfo.keySet
    pathFindersInfo ++= missingRadius.map(radius =>
      radius -> emptyPathFinderInfo(radius).addObstacles(gameState.obstacles.values)
    )
  }

  def addNewObstacle(action: CreateObstacle, gameState: GameState): Unit =
    pathFindersInfo.mapValuesInPlace((radius, pathFinderInfo) =>
      pathFinderInfo.addObstacles(gameState.obstacles.get(action.obstacleId))
    )

  def maybeRemoveObstacle(action: RemoveEntity): Unit =
    pathFindersInfo.mapValuesInPlace((radius, pathFinderInfo) =>
      pathFinderInfo.removeObstaclesByIds(Vector(action.entityId))
    )

  graphsForRadius(initialRadiuses, GameState.empty)
}
