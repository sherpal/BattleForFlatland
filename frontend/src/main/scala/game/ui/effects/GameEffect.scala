package game.ui.effects

import typings.pixiJs.mod.Container

trait GameEffect {

  def destroy(): Unit

  def update(currentTime: Long): Unit

  def isOver(currentTime: Long): Boolean

  def addToContainer(container: Container): Unit

}
