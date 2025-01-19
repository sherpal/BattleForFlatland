package application

import gamelogic.gamestate.GameState

trait GameStateProvider {
  def gameState: GameState
}
