package game.ui.reactivepixi

import com.raquo.airstream.ownership.Owner
import org.scalajs.dom
import typings.pixiJs.PIXI.{Container, DisplayObject, Graphics, Sprite, Texture}
import typings.pixiJs.mod
import typings.pixiJs.mod.Application

import scala.collection.mutable
import scala.language.implicitConversions

trait ReactivePixiElement[+Ref <: DisplayObject] extends Owner {
  val ref: Ref

  private[reactivepixi] val children: mutable.Set[ReactivePixiElement.Base] = mutable.Set.empty

  private def allChildren: Iterator[ReactivePixiElement.Base] =
    children.iterator.flatMap(child => Iterator(child) ++ child.allChildren)

  def amend(mods: PixiModifier[this.type]*): this.type = {
    mods.foreach(_(this))
    this
  }

  private def kill(): Unit = killSubscriptions()

  /**
    * Call this method instead of the destroy method of the underlying [[DisplayObject]].
    *
    * Once this is done, you shouldn't use this reactive pixi element anymore, otherwise unpredictive stuff
    * (mainly bad stuff) will happen.
    */
  def destroy(): Unit = {
    allChildren.foreach(_.kill())
    ref.destroy()
  }

}

object ReactivePixiElement {
  type ReactiveDisplayObject = ReactivePixiElement[DisplayObject]
  type ReactiveContainer     = ReactivePixiElement[Container]
  type ReactiveSprite        = ReactivePixiElement[Sprite]
  type ReactiveGraphics      = ReactivePixiElement[Graphics]

  type Base = ReactiveDisplayObject

  def stage(application: Application) = new ReactiveStage(application)

  def pixiContainer(modifiers: PixiModifier[ReactiveContainer]*): ReactiveContainer = {
    val rc = new ReactiveContainer {
      val ref: Container = new mod.Container
    }
    modifiers.foreach(_(rc))
    rc
  }

  def pixiSprite(texture: Texture, modifiers: PixiModifier[ReactiveSprite]*): ReactiveSprite = {
    val reactiveSprite = new ReactiveSprite {
      val ref: Sprite = new mod.Sprite(texture)
    }
    modifiers.foreach(_(reactiveSprite))
    reactiveSprite
  }

  def pixiGraphics(modifiers: PixiModifier[ReactiveGraphics]*): ReactiveGraphics = {
    val reactiveGraphics = new ReactiveGraphics {
      val ref: Graphics = new mod.Graphics()
    }
    modifiers.foreach(_(reactiveGraphics))
    reactiveGraphics
  }

  implicit def reactiveElementIsModifier[El <: Base](newChild: El): PixiModifier[ReactiveContainer] =
    (element: ReactiveContainer) => addChildTo(element, newChild)

  private[reactivepixi] def addChildTo[El <: ReactiveDisplayObject](element: ReactiveContainer, newChild: El): Unit = {
    element.children += newChild
    element.ref.addChild(newChild.ref)
  }

}
