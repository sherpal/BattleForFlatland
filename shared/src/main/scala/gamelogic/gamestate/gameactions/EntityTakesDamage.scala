package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}

/**
  * Action dealing damage to the entity with specified id.
  * @param id id of the action
  * @param time time of the action
  * @param entityId id of entity taking the damage
  * @param amount positive amount to remove from the current life
  * @param sourceId id of the entity responsible for dealing that damage (used only by possible action transformers).
  *                 (for example, a buff could transform any damage dealt by an entity into heal)
  */
final case class EntityTakesDamage(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    amount: Double,
    sourceId: Entity.Id
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.livingEntityById(entityId).fold(GameStateTransformer.identityTransformer) { entity =>
      new WithEntity(entity.changeLifeTotal(-amount), time) // damage is negative
    }

  def isLegal(gameState: GameState): Option[String] = gameState.livingEntityById(entityId) match {
    case None => Some(s"Entity $entityId does not exist, or it is not a living entity")
    case _    => None
  }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
