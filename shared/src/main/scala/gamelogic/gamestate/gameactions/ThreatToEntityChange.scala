package gamelogic.gamestate.gameactions

import gamelogic.entities.{Entity, WithThreat}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}

/**
  * Changes the threat level that the `sourceId` has towards the `entityId`.
  * The `deltaThreat` (positive or negative) is added to the current threat.
  */
final case class ThreatToEntityChange(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    sourceId: Entity.Id,
    deltaThreat: WithThreat.ThreatAmount,
    isDamageThreat: Boolean
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.withThreatEntityById(entityId).fold(GameStateTransformer.identityTransformer) { entity =>
      new WithEntity(entity.changeThreats(sourceId, deltaThreat, isDamageThreat), time)
    }

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
