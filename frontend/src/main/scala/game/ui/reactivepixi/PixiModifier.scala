package game.ui.reactivepixi

import typings.pixiJs.PIXI.DisplayObject

trait PixiModifier[-El] {

  def apply(element: El): Unit

}
