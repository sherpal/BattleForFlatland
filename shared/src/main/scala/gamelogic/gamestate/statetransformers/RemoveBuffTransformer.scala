package gamelogic.gamestate.statetransformers

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState

/**
  * Removes the buff with given buffId from the bearer with given bearerId.
  */
final class RemoveBuffTransformer(time: Long, bearerId: Entity.Id, buffId: Buff.Id) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = {
    val newBearerBuffs = gameState.buffs.get(bearerId).map(_ - buffId).getOrElse(Map())

    if (newBearerBuffs.isEmpty)
      gameState.copy(time = time, buffs = gameState.buffs - bearerId)
    else
      gameState.copy(time = time, buffs = gameState.buffs + (bearerId -> newBearerBuffs))
  }
}
