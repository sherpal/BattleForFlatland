package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Action dealing damage to the entity with specified id.
  * @param id
  *   id of the action
  * @param time
  *   time of the action
  * @param entityId
  *   id of entity getting healed
  * @param amount
  *   positive amount to add to the current life
  * @param sourceId
  *   id of the entity responsible for the heal (used only by possible action transformers).
  */
final case class EntityGetsHealed(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    amount: Double,
    sourceId: Entity.Id
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.livingEntityById(entityId).fold(GameStateTransformer.identityTransformer) { entity =>
      new WithEntity(entity.changeLifeTotal(amount), time)
    }

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.livingEntityById(entityId) match {
      case None => Some(i18n.entityIsNotLiving(entityId))
      case _ => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
