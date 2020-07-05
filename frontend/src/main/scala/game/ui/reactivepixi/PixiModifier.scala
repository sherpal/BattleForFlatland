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

  val unit: PixiModifier[Any] = (_: Any) => ()

  def factory[El](applyMethod: El => Unit): PixiModifier[El] = (element: El) => applyMethod(element)

  implicit def fromList[El](modifiers: List[PixiModifier[El]]): PixiModifier[El] =
    (element: El) => modifiers.foreach(_(element))

  implicit def fromOption[El](maybeModifier: Option[PixiModifier[El]]): PixiModifier[El] =
    maybeModifier.getOrElse(unit)

}
