package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.{GameAction, GameState}

/** Simply starts the game. */
final case class GameStart(id: Long, time: Long) extends GameAction {

  def apply(gameState: GameState): GameState =
    gameState.copy(time = time, startTime = Some(time))

  def isLegal(gameState: GameState): Boolean = !gameState.started

  def changeId(newId: Id): GameAction = copy(id = newId)
}
