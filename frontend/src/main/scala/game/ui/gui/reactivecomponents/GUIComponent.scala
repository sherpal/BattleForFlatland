package game.ui.gui.reactivecomponents

import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.{PixiModifier, ReactivePixiElement}

import scala.language.implicitConversions

trait GUIComponent {

  val container: ReactivePixiElement.ReactiveContainer = pixiContainer()

}

object GUIComponent {

  implicit def asReactiveContainer(guiComponent: GUIComponent): ReactivePixiElement.ReactiveContainer =
    guiComponent.container

  implicit def asModifier(guiComponent: GUIComponent): PixiModifier[ReactivePixiElement.ReactiveContainer] =
    guiComponent.container

}
