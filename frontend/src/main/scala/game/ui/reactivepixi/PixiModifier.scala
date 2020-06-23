package game.ui.reactivepixi

import scala.language.implicitConversions

trait PixiModifier[-El] {

  def apply(element: El): Unit

  def and[El1 <: El](that: PixiModifier[El1]): PixiModifier[El1] = PixiModifier.factory { element =>
    this.apply(element)
    that.apply(element)
  }

}

object PixiModifier {

  def factory[El](applyMethod: El => Unit): PixiModifier[El] = new PixiModifier[El] {
    def apply(element: El): Unit = applyMethod(element)
  }

  implicit def fromList[El](modifiers: List[PixiModifier[El]]): PixiModifier[El] = new PixiModifier[El] {
    def apply(element: El): Unit = modifiers.foreach(_(element))
  }

}
