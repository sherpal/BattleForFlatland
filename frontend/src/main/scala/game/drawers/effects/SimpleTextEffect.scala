package game.drawers.effects

import gamelogic.gamestate.GameState
import gamelogic.physics.shape.BoundingBox
import utils.misc.RGBAColour
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel

import scala.scalajs.js
import assets.fonts.Fonts

/** Creates a simple effect where the given text follows the given movement in time.
  *
  * @param text
  *   to be displayed
  * @param colour
  *   to give to the text
  * @param startTime
  *   time at which to start the animation
  * @param path
  *   [[game.ui.effects.Path]] to describe the position of the text in function of time. The
  *   position has to be in game world coordinate space.
  */
final class SimpleTextEffect(
    text: String,
    color: Fonts.AllowedColor,
    startTime: Seconds,
    path: Path,
    fontSize: Fonts.AllowedSize = 20
) extends GameEffect {

  val fontKey   = Fonts.fontKeys(color, fontSize)
  val assetName = Fonts.assetNames(color, fontSize)

  val textNode = Text(text, fontKey, Material.Bitmap(assetName))

  def isOver(context: FrameContext[StartupData], viewModel: IndigoViewModel): Boolean =
    path.isOver(context.gameTime.running - startTime)

  def isStarted(currentTime: Seconds): Boolean = currentTime > startTime

  def present(context: FrameContext[StartupData], viewModel: IndigoViewModel): js.Array[SceneNode] =
    val gamePosition  = path(context.gameTime.running - startTime)
    val localPosition = viewModel.gameToLocal(gamePosition)
    js.Array(textNode.withPosition(localPosition))

}
