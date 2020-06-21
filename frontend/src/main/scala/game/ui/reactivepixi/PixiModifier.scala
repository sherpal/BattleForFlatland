package game.ui.reactivepixi

import scala.language.implicitConversions

trait PixiModifier[-El] {

  def apply(element: El): Unit

}

object PixiModifier {

  implicit def fromList[El](modifiers: List[PixiModifier[El]]): PixiModifier[El] = new PixiModifier[El] {
    def apply(element: El): Unit = modifiers.foreach(_(element))
  }

}
