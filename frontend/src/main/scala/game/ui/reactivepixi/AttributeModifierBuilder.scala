package game.ui.reactivepixi

import com.raquo.airstream.core.Observable
import gamelogic.physics.Complex
import typings.pixiJs.PIXI.{Graphics, IHitArea, Rectangle, TextStyle}
import utils.misc.Colour

/**
  * An [[AttributeModifierBuilder]] is the base trait for creating simple [[PixiModifier]] modifying attributes of
  * elements, such as `x`, `y`, `tint` and so on.
  *
  * Builders basically have two methods for creating modifiers: `:=` and `<--`. There are very similar. The difference
  * being that `:=` takes a fixed value whereas `<--` takes an [[com.raquo.airstream.core.Observable]].
  *
  * You also have methods for transforming or combining builders.
  *
  * Ultimately, the "core" implementation of the builder sits in the `modifying` methods, which actually tels how the
  * affected [[ReactivePixiElement]] should be modified.
  *
  * @tparam El type of [[ReactivePixiElement]] that this [[AttributeModifierBuilder]] creates [[PixiModifier]] for.
  *            For example, a `AttributeModifierBuilder[ReactivePixiContainer, _]` creates modifiers for reactive pixi
  *            containers.
  * @tparam A type of the attribute being affected. For example, [[java.lang.Double]] for `x`, [[utils.misc.Colour]] for
  *           `tint`. The type is sometimes a little bit more involved than the js type, in order to increase type
  *           safety. However, a more "close to the metals" alternative should always exist.
  */
trait AttributeModifierBuilder[-El <: ReactivePixiElement.Base, A] {

  def modifying(element: El, a: A): Unit

  def contramap[B](f: B => A): AttributeModifierBuilder[El, B] =
    AttributeModifierBuilder.factory[El, B] { (element, b) =>
      modifying(element, f(b))
    }

  def :=(a: A): Setter[El] = Setter(modifying(_, a))

  def <--(as: Observable[A]): Binder[El] = Binder(
    element => as.foreach(modifying(element, _))(element)
  )

  def zip[El1 <: El, B](that: AttributeModifierBuilder[El1, B]): AttributeModifierBuilder[El1, (A, B)] =
    AttributeModifierBuilder.factory[El1, (A, B)] {
      case (element, (a, b)) =>
        this.modifying(element, a)
        that.modifying(element, b)
    }

}

object AttributeModifierBuilder {

  import ReactivePixiElement._

  /**
    * Creates a modifier builder with the specified modifying function.
    */
  def factory[El <: ReactivePixiElement.Base, A](
      modifying: (El, A) => Unit
  ): AttributeModifierBuilder[El, A] = (element: El, a: A) => modifying(element, a)

  final val x        = factory[ReactiveDisplayObject, Double](_.ref.x = _)
  final val y        = factory[ReactiveDisplayObject, Double](_.ref.y = _)
  final val position = (x zip y).contramap[Complex](_.tuple)

  final val visible     = factory[ReactiveDisplayObject, Boolean](_.ref.visible     = _)
  final val interactive = factory[ReactiveDisplayObject, Boolean](_.ref.interactive = _)

  final val anchor = factory[ReactiveSprite, Double](_.ref.anchor.set(_))
  final val anchorXY = factory[ReactiveSprite, (Double, Double)] {
    case (element, (x, y)) => element.ref.anchor.set(x, y)
  }

  final val scale = factory[ReactiveContainer, Double](_.ref.scale.set(_))
  final val scaleXY = factory[ReactiveContainer, (Double, Double)] {
    case (element, (scaleX, scaleY)) => element.ref.scale.set(scaleX, scaleY)
  }

  final val hitArea = factory[ReactiveDisplayObject, Rectangle] { (element, rectangle) =>
    element.ref.hitArea = rectangle.asInstanceOf[IHitArea]
  }

  final val width  = factory[ReactiveContainer, Double](_.ref.width = _)
  final val height = factory[ReactiveContainer, Double](_.ref.height = _)
  final val dims   = width zip height

  final val tint = factory[ReactiveSprite, Colour] { (element, colour) =>
    element.ref.tint = colour.intColour
  }
  final val tintInt = factory[ReactiveSprite, Int](_.ref.tint = _)

  final val alpha = factory[ReactiveSprite, Double](_.ref.alpha = _)

  final val mask = factory[
    ReactiveDisplayObject,
    ReactiveContainer
  ]((displayObj, container) => displayObj.ref.mask = container.ref)

  /**
    * Same as mask, but automatically add the child to the parent.
    */
  final val maskChild = factory[ReactiveContainer, ReactiveContainer] { (parent, child) =>
    parent.ref.mask = child.ref
    ReactivePixiElement.addChildTo(parent, child)
  }

  /**
    * This is supposed to model a transformation that you may want to apply to your [[typings.pixiJs.PIXI.Graphics]].
    * As such, this transformer can do anything but is primarily thought to change the geometry of the graphics.
    *
    * // todo: probably build an ADT on top of graphics drawing capabilities
    */
  final val moveGraphics = factory[ReactiveGraphics, Graphics => Unit] { (element, effect) =>
    effect(element.ref)
  }

  final val text      = factory[ReactiveText, String](_.ref.text     = _)
  final val textStyle = factory[ReactiveText, TextStyle](_.ref.style = _)

}
