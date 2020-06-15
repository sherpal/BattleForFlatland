package game.ui.effects

import gamelogic.gamestate.GameState
import typings.pixiJs.mod.Container

trait GameEffect {

  def destroy(): Unit

  def update(currentTime: Long, gameState: GameState): Unit

  def isOver(currentTime: Long, gameState: GameState): Boolean

  def addToContainer(container: Container): Unit

}
