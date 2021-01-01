package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Polygon

/**
  * Adds an [[gamelogic.entities.staticstuff.Obstacle]] to the game, with the given parameters
  * (id, position and vertices).
  */
final case class CreateObstacle(
    id: GameAction.Id,
    time: Long,
    obstacleId: Entity.Id,
    position: Complex,
    vertices: Vector[Complex]
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new WithEntity(
    Obstacle(obstacleId, time, position, Polygon(vertices)),
    time
  )

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
