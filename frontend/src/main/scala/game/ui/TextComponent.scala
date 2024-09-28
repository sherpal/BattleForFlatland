package game.ui

import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration
import assets.fonts.Fonts

case class TextComponent(
    text: String,
    anchor: Anchor,
    width: Int,
    height: Int,
    color: Fonts.AllowedColor,
    fontSize: Fonts.AllowedSize,
    textAlign: TextAlignment = TextAlignment.Left,
    visible: Boolean = true
) extends Component {

  val fontKey   = Fonts.fontKeys(color, fontSize)
  val assetName = Fonts.assetNames(color, fontSize)

  def children: js.Array[Component] = js.Array()

  def registerEvents(bounds: Rectangle): scala.scalajs.js.Array[EventRegistration[?]] = js.Array()

  def textNode = Text(text, fontKey, Material.Bitmap(assetName))

  def present(bounds: Rectangle): js.Array[SceneNode] =
    js.Array(
      Text(text, fontKey, Material.Bitmap(assetName))
        .withAlignment(textAlign)
        .withPosition(
          textAlign match
            case indigo.shared.datatypes.TextAlignment.Left => bounds.position
            case TextAlignment.Center => bounds.position + Point(bounds.width / 2, 0)
            case indigo.shared.datatypes.TextAlignment.Right =>
              bounds.position + Point(bounds.width, 0)
        )
    )

}
