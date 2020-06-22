package game.ui.gui.reactivecomponents

import game.ui.reactivepixi.ReactivePixiElement
import game.ui.reactivepixi.ReactivePixiElement._

import scala.language.implicitConversions

trait GUIComponent {

  val container: ReactivePixiElement.ReactiveContainer = pixiContainer()

}

object GUIComponent {

  implicit def asReactiveContainer(guiComponent: GUIComponent): ReactivePixiElement.ReactiveContainer =
    guiComponent.container

}
