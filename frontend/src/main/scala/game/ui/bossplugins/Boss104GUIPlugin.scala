package game.ui.bossplugins

import game.IndigoViewModel
import game.gameutils.toIndigo
import game.ui.components.PlayerFrame
import gamelogic.buffs.boss.boss104.TwinDebuff
import gamelogic.entities.Entity
import indigo.*

import scala.scalajs.js

/** Boss104's twin debuffs must be dispelled from inside the circle of the matching colour, so the
  * healers need to know at a glance which colour each debuffed player carries. The disk beneath the
  * player in the game is not enough: the frame of the bearer is tinted with the debuff colour, and
  * outlined with it.
  */
object Boss104GUIPlugin extends BossGUIPlugin {

  override def playerFrameDecorations(playerId: Entity.Id, bounds: Rectangle, alpha: Double)(using
      viewModel: IndigoViewModel
  ): js.Array[SceneNode] =
    viewModel.gameState
      .allBuffsOfEntity(playerId)
      .collectFirst { case debuff: TwinDebuff => debuff.colour.toIndigo }
      .fold(js.Array[SceneNode]()) { colour =>
        js.Array(
          Shape
            .Box(bounds, Fill.Color(colour.withAlpha(0.45 * alpha)))
            .withDepth(PlayerFrame.overBackgroundDepth),
          // strokes are centred on the box edges: insetting by the stroke width keeps the contour
          // clear of the (red) target outline drawn on the frame's edges
          Shape.Box(
            bounds.contract(contourWidth),
            fill = Fill.Color(RGBA.Zero),
            stroke = Stroke(contourWidth, colour)
          )
        )
      }

  private inline def contourWidth: Int = 4

}
