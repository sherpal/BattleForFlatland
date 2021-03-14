package gamelogic.gamestate.statetransformers

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState

/**
  * Removes the buff with given buffId from the bearer with given bearerId.
  */
final class RemoveBuffTransformer(time: Long, bearerId: Entity.Id, buffId: Buff.Id) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.buffById(bearerId, buffId) match {
      case Some(_: TickerBuff) =>
        val newBearerBuffs = gameState.tickerBuffs.get(bearerId).map(_ - buffId).getOrElse(Map())
        if (newBearerBuffs.isEmpty)
          gameState.copy(newTime = time, tickerBuffs = gameState.tickerBuffs - bearerId)
        else
          gameState.copy(newTime = time, tickerBuffs = gameState.tickerBuffs + (bearerId -> newBearerBuffs))
      case Some(_: PassiveBuff) =>
        val newBearerBuffs = gameState.passiveBuffs.get(bearerId).map(_ - buffId).getOrElse(Map())
        if (newBearerBuffs.isEmpty)
          gameState.copy(newTime = time, passiveBuffs = gameState.passiveBuffs - bearerId)
        else
          gameState.copy(newTime = time, passiveBuffs = gameState.passiveBuffs + (bearerId -> newBearerBuffs))
      case None => gameState
      case Some(buff) =>
        println("wtf!?")
        println(buff)
        throw new Exception("Should never happen")
      // will never happen as the only sub classes are the two above
    }

}
