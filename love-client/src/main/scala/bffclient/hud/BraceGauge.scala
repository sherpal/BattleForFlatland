package bffclient.hud

import bffclient.gui.*
import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Entity
import gamelogic.entities.classes.Square

/** The stacks of our [[BraceShield]], when we are a [[Square]], to be put on the left of our frame.
  */
final class BraceGauge(parent: Frame) {
  private val gauge = StacksGauge(parent, BraceShield.maxStacks.toInt)
  val frame: Frame  = gauge.frame

  /** Shows the stacks of `id`, if it is a living [[Square]]. */
  def update(p: Painter, id: Option[Entity.Id]): Unit = {
    val square = id.flatMap(p.gameState.players.get).collect { case square: Square => square }
    gauge.show(
      square.map(square => BraceShield.stacksOn(p.gameState, square.id).fold(0)(_.toInt)),
      BraceGauge.colour,
      BraceGauge.fullColour
    )
  }
}

object BraceGauge {
  val width: Double = StacksGauge.width

  val colour     = Paint.rgb(95, 155, 230)
  val fullColour = Paint.rgb(150, 200, 255)
}
