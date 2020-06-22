package game.ui.reactivepixi

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner
import org.scalajs.dom
import typings.pixiJs.PIXI.{Container, DisplayObject, Graphics, Rectangle, Sprite, Text, Texture}
import typings.pixiJs.mod
import typings.pixiJs.mod.Application

import scala.collection.mutable
import scala.language.implicitConversions

trait ReactivePixiElement[+Ref <: DisplayObject] extends Owner {
  val ref: Ref

  private[reactivepixi] var destroyCallbacks: Vector[() => Unit] = Vector(() => killSubscriptions())

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
    kill()
    ref.destroy()
  }

}

object ReactivePixiElement {
  type ReactiveDisplayObject = ReactivePixiElement[DisplayObject]
  type ReactiveContainer     = ReactivePixiElement[Container]
  type ReactiveSprite        = ReactivePixiElement[Sprite]
  type ReactiveGraphics      = ReactivePixiElement[Graphics]
  type ReactiveText          = ReactivePixiElement[Text]

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

  def pixiText(text: String, modifiers: PixiModifier[ReactiveText]*): ReactiveText = {
    val reactiveText = new ReactiveText {
      val ref: Text = new mod.Text(text)
    }
    modifiers.foreach(_(reactiveText))
    reactiveText
  }

  implicit def reactiveElementIsModifier[El <: Base](newChild: El): PixiModifier[ReactiveContainer] =
    (element: ReactiveContainer) => addChildTo(element, newChild)

  private[reactivepixi] def addChildTo[El <: ReactiveDisplayObject](element: ReactiveContainer, newChild: El): Unit = {
    element.destroyCallbacks :+= { () =>
      newChild.kill()
    }
    element.ref.addChild(newChild.ref)
  }

}
