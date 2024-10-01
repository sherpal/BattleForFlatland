package game.drawers

import indigo.*
import assets.Asset
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

class LoopingAnimatedSprite(
    asset: Asset,
    frameDurationMillis: Long,
    crops: js.Array[Rectangle],
    maybeAlpha: Option[Double]
) {

  private val m = maybeAlpha match {
    case Some(alpha) => Material.ImageEffects(asset.assetName).withAlpha(alpha)
    case None        => Material.Bitmap(asset.assetName)
  }

  private val theGraphic = Graphic(asset.size, m)

  def graphic(time: Long, targetSize: Size) =
    val cropIndex   = (time / frameDurationMillis) % crops.length
    val currentCrop = crops(cropIndex.toInt)
    theGraphic
      .withCrop(currentCrop)
      .withScale(targetSize.toVector / currentCrop.size.toVector)

}

object LoopingAnimatedSprite {

  def apply(
      asset: Asset,
      frameDurationMillis: Long,
      nbrRows: Int,
      nbrCols: Int,
      maybeAlpha: Option[Double] = None
  ): LoopingAnimatedSprite = {
    val imageWidth  = asset.size.width / nbrCols
    val imageHeight = asset.size.height / nbrRows
    new LoopingAnimatedSprite(
      asset,
      frameDurationMillis,
      for {
        row <- (0 until nbrRows).toJSArray
        col <- (0 until nbrCols).toJSArray
      } yield Rectangle(Point(col * imageWidth, row * imageHeight), Size(imageWidth, imageHeight)),
      maybeAlpha
    )
  }

}
