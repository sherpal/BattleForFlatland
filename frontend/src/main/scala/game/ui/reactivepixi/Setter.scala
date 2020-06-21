package game.ui.reactivepixi

import typings.pixiJs.PIXI.DisplayObject

trait Setter[-El <: DisplayObject] extends PixiModifier[El]

object Setter {

  def apply[El <: DisplayObject](setAttribute: El => Unit): Setter[El] = (element: El) => setAttribute(element)

}
