package game.ui

import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration

case class TextComponent(
    text: String,
    fontSize: Pixels,
    anchor: Anchor,
    color: RGBA,
    width: Int,
    height: Int,
    textAlign: TextAlign = TextAlign.Left,
    visible: Boolean = true
) extends Component {

  def children: js.Array[Component] = js.Array()

  def registerEvents(bounds: Rectangle): scala.scalajs.js.Array[EventRegistration[?]] = js.Array()

  def present(bounds: Rectangle): js.Array[SceneNode] = js.Array(
    TextBox(text, width, height)
      .withFontFamily(FontFamily.cursive)
      .withColor(color)
      .withFontSize(fontSize)
      .withPosition(bounds.position)
      .modifyStyle(_.withAlign(textAlign))
  )

}
