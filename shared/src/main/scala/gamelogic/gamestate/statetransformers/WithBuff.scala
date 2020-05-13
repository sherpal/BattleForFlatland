package gamelogic.gamestate.statetransformers

import gamelogic.buffs.Buff
import gamelogic.gamestate.GameState

final class WithBuff(buff: Buff) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = {
    val newBuffMap = gameState.buffs.getOrElse(buff.bearerId, Map())
    gameState.copy(
      time  = buff.appearanceTime,
      buffs = gameState.buffs + (buff.bearerId -> (newBuffMap + (buff.buffId -> buff)))
    )
  }
}
