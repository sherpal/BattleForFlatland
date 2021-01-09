package game.ui.effects.visualeffects

import game.ui.effects.GameEffect

import com.raquo.laminar.api.L._

import typings.canvasConfetti.mod.{Options, Origin}

import scala.concurrent.duration._
import scala.scalajs.js

import gamelogic.gamestate.GameState
import typings.pixiJs.mod.Container
import scala.util.Random

import scala.scalajs.js.annotation.JSImport

final class ConfettiEffect extends GameEffect {

  val duration     = 15.seconds
  val animationEnd = System.currentTimeMillis() + duration.toMillis

  def defaultOptions =
    Options()
      .setStartVelocity(30)
      .setSpread(360)
      .setTicks(60)
      .setZIndex(5)

  private class KillableOwner extends Owner {
    def kill(): Unit = killSubscriptions()
  }
  implicit private val owner: KillableOwner = new KillableOwner

  private val newConfettiBus: EventBus[Long] = new EventBus

  private def optionsWithXBetween(particleCount: Double, minX: Double, maxX: Double) =
    defaultOptions
      .setParticleCount(particleCount)
      .setOrigin(Origin().setX(Random.between(minX, maxX)).setY(Random.nextDouble() - 0.2))

  newConfettiBus.events.throttle(250).foreach { (currentTime: Long) =>
    val timeLeft = animationEnd - currentTime

    val particleCount = 50.0 * (timeLeft.toDouble / duration.toMillis)
    confetti(optionsWithXBetween(particleCount, 0.1, 0.3))
    confetti(optionsWithXBetween(particleCount, 0.7, 0.9))
  }

  def destroy(): Unit =
    owner.kill()

  def update(currentTime: Long, gameState: GameState): Unit =
    newConfettiBus.writer.onNext(currentTime)

  def isOver(currentTime: Long, gameState: GameState): Boolean =
    currentTime > animationEnd

  def addToContainer(container: Container): Unit = ()

}

object ConfettiEffect {

  private val dummyGameState = GameState.empty

  private def iterate(confettiEffect: ConfettiEffect): Unit = {
    val now = System.currentTimeMillis()

    if (!confettiEffect.isOver(now, dummyGameState)) {
      confettiEffect.update(now, dummyGameState)
      scala.scalajs.js.timers.setTimeout(100.millis) {
        iterate(confettiEffect)
      }
    }
  }

  def runAsStandAlone(): Unit = {
    val confettiEffect = new ConfettiEffect
    iterate(confettiEffect)
  }

}
