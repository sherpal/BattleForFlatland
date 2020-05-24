package game.ui.gui.components

import gamelogic.gamestate.GameState
import typings.pixiJs.mod.Container

trait GUIComponent {

  val container: Container = new Container

  /** Updates the component at the given time, for the given game state. */
  def update(gameState: GameState, currentTime: Long): Unit

}
