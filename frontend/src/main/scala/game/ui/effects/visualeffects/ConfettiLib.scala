package game.ui.effects.visualeffects

import game.ui.effects.GameEffect

import com.raquo.laminar.api.L._

import typings.canvasConfetti.canvasConfettiRequire
//import typings.canvasConfetti.mod.{^ => confetti}
import typings.canvasConfetti.canvasConfettiRequire
import typings.canvasConfetti.mod.{Options, Origin}

import scala.concurrent.duration._
import scala.scalajs.js

import gamelogic.gamestate.GameState
import typings.pixiJs.mod.Container
import scala.util.Random

import scala.scalajs.js.annotation.JSImport

@JSImport("canvas-confetti", JSImport.Default)
@js.native
object ConfettiLib extends js.Object {

  def apply(options: Options): Unit = js.native
}
