package game.drawers.effects

import indigo.*
import scala.scalajs.js
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel

trait GameEffect {

  def present(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): js.Array[SceneNode]

  def isOver(context: FrameContext[StartupData], viewModel: IndigoViewModel): Boolean

}
