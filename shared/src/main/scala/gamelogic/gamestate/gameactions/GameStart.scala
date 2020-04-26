package gamelogic.gamestate.gameactions

import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Simply starts the game. */
final case class GameStart(id: Long, time: Long, pos: Complex) extends GameAction {

  def apply(gameState: GameState): GameState =
    gameState.copy(time = time, startTime = Some(time))

  def isLegal(gameState: GameState): Boolean = !gameState.started
}
