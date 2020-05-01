package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.{GameAction, GameState}

final case class UpdateTimestamp(id: Long, time: Long) extends GameAction {
  def apply(gameState: GameState): GameState = gameState.timeUpdate(time)

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
