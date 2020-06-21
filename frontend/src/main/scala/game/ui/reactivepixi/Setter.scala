package game.ui.reactivepixi

trait Setter[-El <: ReactivePixiElement.Base] extends PixiModifier[El]

object Setter {

  def apply[El <: ReactivePixiElement.Base](setAttribute: El => Unit): Setter[El] =
    (element: El) => setAttribute(element)

}
