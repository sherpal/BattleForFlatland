package gamelogic.gamestate.statetransformers

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState

final class RemoveEntityTransformer(entityId: Entity.Id, time: Long) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(newTime = time, entities = gameState.entities - entityId)
}
