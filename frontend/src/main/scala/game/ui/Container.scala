package game.ui

import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration

trait Container(
    val width: Int,
    val height: Int,
    val anchor: Anchor = Anchor.topLeft
) extends Component {

  override final def present(bounds: Rectangle): js.Array[SceneNode] = js.Array()

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] =
    js.Array()

  override def visible: Boolean = true

}