package game.ai.utils.pathfinders
import gamelogic.entities.Entity.Id
import gamelogic.entities.PolygonBody
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{CreateObstacle, RemoveEntity}

final class OnlyObstaclesPathFinder(val entityRadius: Double) extends PathFinder {
  def maybeNewObstacle(action: GameAction, gameState: GameState): Option[PolygonBody] = action match {
    case CreateObstacle(_, _, id, _, _) =>
      gameState.obstacles.get(id)
    case _ => None
  }

  def maybeRemoveObstacle(action: GameAction): Option[Id] = action match {
    case RemoveEntity(id, _, _) => Some(id)
    case _                      => None
  }

  def obstaclesFromGameState(gameState: GameState): List[PolygonBody] = gameState.obstacles.valuesIterator.toList
}
