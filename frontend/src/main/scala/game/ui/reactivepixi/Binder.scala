package game.ui.reactivepixi

import com.raquo.airstream.ownership.Subscription
import typings.pixiJs.PIXI.DisplayObject

trait Binder[-El <: DisplayObject] extends PixiModifier[El] {

  def bind(element: El): Unit

  override def apply(element: El): Unit = bind(element)

}

object Binder {

  def apply[El <: DisplayObject](binding: El => Subscription): Binder[El] = (element: El) => binding(element)

}
