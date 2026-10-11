package game.drawers.effects

import assets.Asset
import game.IndigoViewModel
import game.scenes.ingame.InGameScene.StartupData
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex
import indigo.*

import scala.scalajs.js

final class FlashingShape(
    position: Complex,
    rotation: Angle,
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
        .withRotation(Radians(-rotation.value))
        .withPosition(viewModel.gameToLocal(position))
        .withScale(size.toVector / currentCrop.size.toVector)
    )

}
