package game.ui.effects.visualeffects

import game.ui.effects.GameEffect

import com.raquo.laminar.api.L._

//import typings.canvasConfetti.canvasConfettiRequire
//import typings.canvasConfetti.mod.{^ => confetti}
//import typings.canvasConfetti.canvasConfettiRequire
import typings.canvasConfetti.mod.{Options, Origin}

import scala.concurrent.duration._
import scala.scalajs.js

import gamelogic.gamestate.GameState
import typings.pixiJs.mod.Container
import scala.util.Random

import scala.scalajs.js.annotation.JSImport

final class Confetti extends GameEffect {

  val duration     = 15.seconds
  val animationEnd = System.currentTimeMillis() + duration.toMillis
  //var defaults = { startVelocity: 30, spread: 360, ticks: 60, zIndex: 0 };

  def defaultOptions =
    Options()
      .setStartVelocity(30)
      .setSpread(360)
      .setTicks(60)
      .setZIndex(5)

  private implicit val owner: Owner = new Owner {}

  private val newConfettiBus: EventBus[Long] = new EventBus

  newConfettiBus.events.throttle(250).foreach { (currentTime: Long) =>
    val timeLeft = animationEnd - currentTime

    val particleCount = 50.0 * (timeLeft.toDouble / duration.toMillis)
    ConfettiLib(
      defaultOptions
        .setParticleCount(particleCount)
        .setOrigin(Origin().setX(Random.between(0.1, 0.3)).setY(Random.nextDouble() - 0.2))
    )
    ConfettiLib(
      defaultOptions
        .setParticleCount(particleCount)
        .setOrigin(Origin().setX(Random.between(0.7, 0.9)).setY(Random.nextDouble() - 0.2))
    )
  }

  def destroy(): Unit = ()

  def update(currentTime: Long, gameState: GameState): Unit =
    newConfettiBus.writer.onNext(currentTime)

  def isOver(currentTime: Long, gameState: GameState): Boolean =
    currentTime > animationEnd

  def addToContainer(container: Container): Unit = ()

}

object Confetti {

  private val dummyGameState = GameState.empty

  private def iterate(confettiEffect: Confetti): Unit = {
    val now = System.currentTimeMillis()

    if (!confettiEffect.isOver(now, dummyGameState)) {
      confettiEffect.update(now, dummyGameState)
      scala.scalajs.js.timers.setTimeout(100.millis) {
        iterate(confettiEffect)
      }
    }
  }

  def runAsStandAlone(): Unit = {
    val confettiEffect = new Confetti
    iterate(confettiEffect)
  }

}
