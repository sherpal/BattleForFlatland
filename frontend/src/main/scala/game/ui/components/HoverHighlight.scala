package game.ui.components

import game.IndigoViewModel
import indigo.*

import scala.scalajs.js

/** Discrete highlight of a unit's component, to show that "mouse over" would act on that unit. */
object HoverHighlight {

  private val glowColour = RGBA.fromColorInts(80, 140, 255)
  private val glowSteps  = 6

  /** Whether the highlight must be displayed for a component with these bounds. It is only the case
    * when the mouse over feature is enabled, otherwise it would hint at something that won't
    * happen.
    */
  def isHovered(bounds: Rectangle)(using viewModel: IndigoViewModel): Boolean =
    viewModel.startupData.controls.mouseOverEnabled &&
      bounds.isPointWithin(viewModel.localMousePos)

  /** A nearly transparent tint, with a glow that fades from the edges towards the inside, and so
    * is the strongest in the corners.
    */
  def nodes(bounds: Rectangle, alpha: Double, depth: Depth): js.Array[SceneNode] = {
    val tint = Shape
      .Box(bounds, Fill.Color(glowColour.withAlpha(0.08 * alpha)))
      .withDepth(depth)

    val glow = (0 until glowSteps).map { step =>
      val stepAlpha = 0.3 * alpha * (1 - step.toDouble / glowSteps)
      Shape
        .Box(
          bounds.contract(step),
          Fill.Color(RGBA.Zero),
          Stroke(1, glowColour.withAlpha(stepAlpha))
        )
        .withDepth(depth)
    }

    js.Array[SceneNode](tint) ++ js.Array[SceneNode](glow*)
  }

}
