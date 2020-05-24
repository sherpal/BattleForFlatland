package gamelogic.gamestate.gameactions

import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}

final case class EntityResourceChanges(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    amount: Double,
    resource: Resource
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.withAbilityEntitiesById(entityId).fold(GameStateTransformer.identityTransformer) { entity =>
      new WithEntity(entity.resourceAmountChange(ResourceAmount(amount, resource)), time)
    }

  def isLegal(gameState: GameState): Boolean = gameState.withAbilityEntitiesById(entityId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
