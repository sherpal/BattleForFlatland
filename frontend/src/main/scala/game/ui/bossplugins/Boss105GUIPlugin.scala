package game.ui.bossplugins

import game.IndigoViewModel
import game.ui.components.PlayerFrame
import gamelogic.buffs.boss.boss105.VolatileCharge
import gamelogic.entities.Entity
import indigo.*

import scala.scalajs.js

/** The bearer of a volatile charge must either be dispelled or go away from everybody, so the
  * healers and the Pentagons need to see it from the frames: the frame of the bearer is tinted and
  * outlined in orange, more and more opaque as the charge gets closer to exploding.
  */
object Boss105GUIPlugin extends BossGUIPlugin {

  private val chargeColour = RGBA.fromColorInts(255, 110, 20)

  override def playerFrameDecorations(playerId: Entity.Id, bounds: Rectangle, alpha: Double)(using
      viewModel: IndigoViewModel
  ): js.Array[SceneNode] =
    viewModel.gameState
      .allBuffsOfEntity(playerId)
      .collectFirst { case charge: VolatileCharge => charge }
      .fold(js.Array[SceneNode]()) { charge =>
        val elapsed =
          ((viewModel.gameState.time - charge.appearanceTime).toDouble / VolatileCharge.duration max 0.0) min 1.0
        js.Array(
          Shape
            .Box(bounds, Fill.Color(chargeColour.withAlpha((0.25 + 0.5 * elapsed) * alpha)))
            .withDepth(PlayerFrame.overBackgroundDepth),
          Shape.Box(
            bounds.contract(contourWidth),
            fill = Fill.Color(RGBA.Zero),
            stroke = Stroke(contourWidth, chargeColour)
          )
        )
      }

  private inline def contourWidth: Int = 4

}
