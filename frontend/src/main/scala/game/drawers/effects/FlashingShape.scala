package game.drawers.effects

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape
import assets.Asset
import indigo.*
import game.scenes.ingame.InGameScene.StartupData
import game.IndigoViewModel

import scala.scalajs.js

final class FlashingShape(
    position: Complex,
    rotation: Double,
    startTime: Seconds,
    duration: Seconds,
    animationsCrops: js.Array[Rectangle],
    texture: Asset,
    size: Size
) extends GameEffect {

  override def isOver(context: FrameContext[StartupData], viewModel: IndigoViewModel): Boolean =
    context.gameTime.running - startTime > duration

  override def present(
      context: FrameContext[StartupData],
      viewModel: IndigoViewModel
  ): scala.scalajs.js.Array[SceneNode] =
    val running = context.gameTime.running - startTime
    val currentCropIndex =
      (running / duration * animationsCrops.length).toInt.max(0).min(animationsCrops.length - 1)
    val currentCrop = animationsCrops(currentCropIndex)

    js.Array(
      Graphic(
        indigo.Rectangle(size),
        1,
        Material.Bitmap(texture.assetName)
      ).withCrop(currentCrop)
        .withRef(currentCrop.size.toPoint / 2)
        .withRotation(Radians(-rotation))
        .withPosition(viewModel.gameToLocal(position))
        .withScale(size.toVector / currentCrop.size.toVector)
    )

}
