package game.ui.effects.visualeffects

import scala.language.implicitConversions

import org.scalajs.dom
import scala.scalajs.js

@js.native
trait WindowConfetti extends js.Object {

  def confetti(options: ConfettiOptions): Unit = js.native

}

object WindowConfetti {

  implicit def windowConfetti(window: dom.Window): WindowConfetti =
    window.asInstanceOf[WindowConfetti]

}
