package game.ui.reactivepixi

import com.raquo.airstream.core.Observable
import typings.pixiJs.PIXI.DisplayObject

trait AttributeModifierBuilder[-El <: DisplayObject, A] {

  def :=(a: A): Setter[El]

  def <--(as: Observable[A]): Binder[DisplayObject]

}

object AttributeModifierBuilder {

  object x extends AttributeModifierBuilder[DisplayObject, Double] {
    def :=(a: Double): Setter[DisplayObject] = Setter(_.x = a)

    def <--(as: Observable[Double]): Binder[DisplayObject] = Binder(
      element => as.foreach(element.x = _)(???)
    )
  }

}
