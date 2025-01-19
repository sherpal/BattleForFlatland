package game.ui.components

import game.ui.Component
import game.ui.Anchor
import indigo.Rectangle
import indigo.SceneNode
import game.ui.Component.EventRegistration
import scala.scalajs.js
import indigo.*

final case class GraphicComponent(
    height: Int,
    width: Int,
    graphic: (Rectangle, Double) => Graphic[Material],
    anchor: Anchor = Anchor.topLeft
) extends Component {

  override def visible: Boolean = true

  override def registerEvents(bounds: Rectangle): js.Array[EventRegistration[?]] = js.Array()

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] =
    js.Array(graphic(bounds, alpha))

  override def children: js.Array[Component] = js.Array()

  override def alpha: Double = 1.0

}
