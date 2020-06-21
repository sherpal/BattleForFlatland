package game.ui.reactivepixi

import com.raquo.airstream.ownership.Owner
import typings.pixiJs.PIXI.DisplayObject

final class ReactivePixiElement[+Ref <: DisplayObject](val ref: Ref) extends Owner {}
