package game.ui.components

import indigo.*
import game.ui.*

import scala.scalajs.js
import assets.Asset
import game.ui.components.StatusBar.Horizontal
import game.ui.components.StatusBar.Vertical
import game.ui.Component.EventRegistration

final case class StatusBar(
    value: Double,
    maxValue: Double,
    color: Double => RGBA,
    asset: Asset,
    orientation: StatusBar.Orientation,
    width: Int,
    height: Int,
    anchor: Anchor
) extends Component {

  def children = js.Array()

  override def visible: Boolean = true

  override def present(bounds: Rectangle): js.Array[SceneNode] = {
    val barValue = (value / maxValue).max(0.0).min(1.0)
    val (position, cropPosition, cropSize) = orientation match {
      case StatusBar.Horizontal =>
        (bounds.position, Point(0, 0), Size((asset.size.width * barValue).toInt, asset.size.height))
      case StatusBar.Vertical =>
        (
          bounds.position + Point(0, math.ceil((1 - barValue) * bounds.size.height).toInt),
          Point(0, math.ceil((1 - barValue) * asset.size.height).toInt),
          Size(asset.size.width, math.ceil(asset.size.height * barValue).toInt)
        )
    }

    val theColor = color(barValue)

    js.Array(
      Graphic(
        Rectangle(asset.size),
        1,
        Material
          .ImageEffects(asset.assetName)
          .withTint(theColor)
          .withAlpha(theColor.a)
      ).withPosition(position)
        .withScale(asset.scaleTo(bounds.size))
        .withCrop(Rectangle(cropPosition, cropSize))
    )

  }

  override def registerEvents(parentRectangle: Rectangle): js.Array[EventRegistration[?]] =
    js.Array()

  override def propagateToChildren: Boolean = false

}

object StatusBar {
  sealed trait Orientation
  case object Horizontal extends Orientation
  case object Vertical   extends Orientation
}
