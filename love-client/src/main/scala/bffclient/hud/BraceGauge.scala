package bffclient.hud

import bffclient.gui.*
import bffclient.hud.HudWidgets.*
import bffclient.render.Painter
import bffclient.ui.Colour as Paint
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Entity
import gamelogic.entities.classes.Square

/** The stacks of our [[BraceShield]], when we are a [[Square]]: a vertical gauge of one segment per
  * possible stack, filled from the bottom, to be put on the left of our frame.
  */
final class BraceGauge(parent: Frame) {
  val frame: Frame = panel(parent, radius = 5)
  frame.setSize(BraceGauge.width, Hud.frameHeight)

  private val padding   = 3.0
  private val spacing   = 2.0
  private val maxStacks = BraceShield.maxStacks.toInt
  private val segmentHeight =
    (Hud.frameHeight - 2 * padding - (maxStacks - 1) * spacing) / maxStacks

  /** From the bottom up. */
  private val segments = (0 until maxStacks).map { index =>
    val segment = frame.createTexture(HudStyle.trackColour, Layer.Artwork)
    val y       = Hud.frameHeight - padding - (index + 1) * segmentHeight - index * spacing
    place(segment, frame, padding, y, BraceGauge.width - 2 * padding, segmentHeight)
    segment.setCornerRadius(2)
    segment
  }

  /** Shows the stacks of `id`, if it is a living [[Square]]. */
  def update(p: Painter, id: Option[Entity.Id]): Unit = {
    val square = id.flatMap(p.gameState.players.get).collect { case square: Square => square }
    frame.setShown(square.isDefined)
    for square <- square do {
      val stacks = BraceShield.stacksOn(p.gameState, square.id).fold(0)(_.toInt)
      val colour = if stacks >= maxStacks then BraceGauge.fullColour else BraceGauge.colour
      for (segment, index) <- segments.zipWithIndex do
        segment.setColour(if index < stacks then colour else HudStyle.trackColour)
    }
  }
}

object BraceGauge {
  val width = 14.0

  val colour     = Paint.rgb(95, 155, 230)
  val fullColour = Paint.rgb(150, 200, 255)
}
