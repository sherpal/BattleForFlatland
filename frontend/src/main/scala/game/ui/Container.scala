package game.ui

import indigo.*
import scala.scalajs.js
import game.ui.Component.EventRegistration

trait Container(
    val width: Int,
    val height: Int,
    val anchor: Anchor = Anchor.topLeft,
    val alpha: Double = 1.0
) extends Component {

  override final def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] =
    js.Array()

  override def visible: Boolean = true

}
