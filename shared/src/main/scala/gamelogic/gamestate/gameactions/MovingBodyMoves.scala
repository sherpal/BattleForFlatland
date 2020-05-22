package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class MovingBodyMoves(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    direction: Angle,
    rotation: Angle,
    speed: Double,
    moving: Boolean
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .movingBodyEntityById(entityId)
      .fold(GameStateTransformer.identityTransformer) { movingBody =>
        new WithEntity(movingBody.move(time, position, direction, rotation, speed, moving))
      }

  def isLegal(gameState: GameState): Boolean = gameState.movingBodyEntityById(entityId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
