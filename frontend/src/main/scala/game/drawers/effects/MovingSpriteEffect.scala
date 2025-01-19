package game.drawers.effects

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.BoundingBox
import indigo.*
import assets.Asset
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel
import scala.scalajs.js

/** Creates an effect which follows the given `path`. A sprite with the given `texture` is created.
  * The position in the world of this effect will be the given by the path current position,
  * translated by the `worldAnchorPosition` function.
  *
  * @param texture
  *   texture for the effect sprite
  * @param startTime
  *   starting time of the effect
  * @param path
  *   path that the effect must follow
  * @param rotation
  *   gives the rotation of the sprite as function of current [[gamelogic.gamestate.GameState]] and
  *   time
  * @param worldAnchorPosition
  *   translation of the effect given the current [[gamelogic.gamestate.GameState]] and time
  * @param anchor
  *   anchor to set the sprite of
  */
final class MovingSpriteEffect(
    texture: Asset,
    startTime: Seconds,
    path: Path,
    runningToTimeDelta: Long,
    rotation: (GameState, Seconds) => Double,
    worldAnchorPosition: (GameState, Long) => Complex,
    anchor: (Double, Double) = (0.5, 0.5)
) extends GameEffect {

  def present(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): scala.scalajs.js.Array[SceneNode] = {
    val now     = context.gameTime.running.toMillis.toLong + runningToTimeDelta
    val running = context.gameTime.running - startTime
    val imageRotation = Radians(
      -rotation(viewModel.gameState, running)
    )
    val position = path(running) + worldAnchorPosition(viewModel.gameState, now)

    val localPosition = viewModel.gameToLocal(position)

    js.Array(
      texture.indigoGraphic(
        localPosition,
        None,
        imageRotation,
        texture.size
      )
    )
  }

  def isOver(context: FrameContext[StartupData], viewModel: IndigoViewModel): Boolean =
    path.isOver(context.gameTime.running - startTime)
}
