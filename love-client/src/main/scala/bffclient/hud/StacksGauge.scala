package bffclient.hud

import bffclient.gui.*
import bffclient.hud.HudWidgets.*
import bffclient.ui.Colour as Paint

/** A vertical gauge of one segment per possible stack (up to `maxStacks`), filled from the bottom,
  * as tall as a unit frame. It is the shape of the stacks that the classes keep up: the shield of
  * the Square ([[BraceGauge]]), the facets and the steady aim of the Pentagon ([[FacetsGauge]],
  * [[SteadyAimGauge]]).
  */
final class StacksGauge(parent: Frame, maxStacks: Int, width: Double = StacksGauge.width) {
  val frame: Frame = panel(parent, radius = 5)
  frame.setSize(width, Hud.frameHeight)

  private val padding = 3.0
  private val spacing = 2.0
  private val segmentHeight =
    (Hud.frameHeight - 2 * padding - (maxStacks - 1) * spacing) / maxStacks

  /** From the bottom up. */
  private val segments = (0 until maxStacks).map { index =>
    val segment = frame.createTexture(HudStyle.trackColour, Layer.Artwork)
    val y       = Hud.frameHeight - padding - (index + 1) * segmentHeight - index * spacing
    place(segment, frame, padding, y, width - 2 * padding, segmentHeight)
    segment.setCornerRadius(2)
    segment
  }

  /** Shows `stacks` (hiding the gauge when there are none to show), in `fullColour` when they are
    * all there.
    */
  def show(stacks: Option[Int], colour: Paint, fullColour: Paint): Unit = {
    frame.setShown(stacks.isDefined)
    for stacks <- stacks do {
      val filled = if stacks >= maxStacks then fullColour else colour
      for (segment, index) <- segments.zipWithIndex do
        segment.setColour(if index < stacks then filled else HudStyle.trackColour)
    }
  }
}

object StacksGauge {
  val width = 14.0

  /** For two gauges side by side (the Pentagon's), so that they do not reach the team's frames on
    * the left.
    */
  val narrowWidth = 11.0
}
