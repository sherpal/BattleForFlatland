package gamelogic.gamestate.statetransformers

import gamelogic.gamestate.GameState

/**
  * Modifies the time of the [[gamelogic.gamestate.GameState]], leaving all other properties untouched.
  */
final class UpdateTimeTransformer(newTime: Long) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = gameState.copy(newTime = newTime)
}
