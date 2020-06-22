package game.ui.reactivepixi

import com.raquo.airstream.core.Observable
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.{DisplayObject, Graphics, IHitArea, Rectangle}
import utils.misc.Colour

trait AttributeModifierBuilder[-El <: ReactivePixiElement.Base, A] {

  def :=(a: A): Setter[El]

  def <--(as: Observable[A]): Binder[El]

}

object AttributeModifierBuilder {

  import ReactivePixiElement._

  /**
    * Creates a modifier builder with the specified modifying function.
    */
  def attributeModifierBuilderFactory[El <: ReactivePixiElement.Base, A](
      modifying: (El, A) => Unit
  ): AttributeModifierBuilder[El, A] =
    new AttributeModifierBuilder[El, A] {
      def :=(a: A): Setter[El] = Setter(modifying(_, a))

      def <--(as: Observable[A]): Binder[El] = Binder(
        element => as.foreach(modifying(element, _))(element)
      )
    }

  final val x = attributeModifierBuilderFactory[ReactiveDisplayObject, Double](_.ref.x = _)
  final val y = attributeModifierBuilderFactory[ReactiveDisplayObject, Double](_.ref.y = _)
  final val position = attributeModifierBuilderFactory[ReactiveDisplayObject, Complex] { (element, pos) =>
    element.ref.x = pos.re
    element.ref.y = pos.im
  }
  final val visible = attributeModifierBuilderFactory[ReactiveDisplayObject, Boolean](_.ref.visible = _)

  final val anchor = attributeModifierBuilderFactory[ReactiveSprite, Double](_.ref.anchor.set(_))
  final val anchorXY = attributeModifierBuilderFactory[ReactiveSprite, (Double, Double)] {
    case (element, (x, y)) => element.ref.anchor.set(x, y)
  }

  final val scale = attributeModifierBuilderFactory[ReactiveContainer, Double](_.ref.scale.set(_))
  final val scaleXY = attributeModifierBuilderFactory[ReactiveContainer, (Double, Double)] {
    case (element, (scaleX, scaleY)) => element.ref.scale.set(scaleX, scaleY)
  }

  final val hitArea = attributeModifierBuilderFactory[ReactiveDisplayObject, Rectangle] { (element, rectangle) =>
    element.ref.hitArea = rectangle.asInstanceOf[IHitArea]
  }

  final val width  = attributeModifierBuilderFactory[ReactiveContainer, Double](_.ref.width  = _)
  final val height = attributeModifierBuilderFactory[ReactiveContainer, Double](_.ref.height = _)

  final val tint = attributeModifierBuilderFactory[ReactiveSprite, Colour] { (element, colour) =>
    element.ref.tint = colour.intColour
  }
  final val tintInt = attributeModifierBuilderFactory[ReactiveSprite, Int](_.ref.tint = _)

  final val mask = attributeModifierBuilderFactory[
    ReactiveDisplayObject,
    ReactiveContainer
  ]((displayObj, container) => displayObj.ref.mask = container.ref)

  /**
    * Same as mask, but automatically add the child to the parent.
    */
  final val maskChild = attributeModifierBuilderFactory[ReactiveContainer, ReactiveContainer] { (parent, child) =>
    parent.ref.mask = child.ref
    ReactivePixiElement.addChildTo(parent, child)
  }

  /**
    * This is supposed to model a transformation that you may want to apply to your [[typings.pixiJs.PIXI.Graphics]].
    * As such, this transformer can do anything but is primarily thought to change the geometry of the graphics.
    *
    * // todo: probably build an ADT on top of graphics drawing capabilities
    */
  final val moveGraphics = attributeModifierBuilderFactory[ReactiveGraphics, Graphics => Unit] { (element, effect) =>
    effect(element.ref)
  }

}
