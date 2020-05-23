package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}

/**
  * Action that simply change the current target of the specified entity.
  * @param entityId id of the entity in the game. It has to be a [[gamelogic.entities.WithTarget]] instance otherwise
  *                 this action is illegal (and does nothing)
  * @param newTargetId the entity id of the new target
  */
final case class ChangeTarget(id: GameAction.Id, time: Long, entityId: Entity.Id, newTargetId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .withTargetEntityById(entityId)
      .fold(
        GameStateTransformer.identityTransformer
      ) { entity =>
        new WithEntity(entity.changeTarget(newTargetId))
      }

  def isLegal(gameState: GameState): Boolean = gameState.withTargetEntityById(entityId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
