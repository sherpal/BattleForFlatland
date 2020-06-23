package game.ui.reactivepixi

import com.raquo.airstream.ownership.Subscription

trait Binder[-El <: ReactivePixiElement.Base] extends PixiModifier[El] {

  def bind(element: El): Subscription

  override def apply(element: El): Unit = bind(element)

}

object Binder {

  def apply[El <: ReactivePixiElement.Base](binding: El => Subscription): Binder[El] = (element: El) => binding(element)

}
