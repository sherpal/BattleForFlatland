package gamelogic.gamestate.gameactions

import gamelogic.gamestate.{GameAction, GameState}

/** Simply ends the game. */
final case class EndGame(id: Long, time: Long) extends GameAction {
  def apply(gameState: GameState): GameState = gameState.copy(time = time, endTime = Some(time))
}
