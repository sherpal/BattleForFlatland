package game.ui.components.grid

import indigo.*
import game.ui.*

import scala.scalajs.js

case class GridCell(child: Component, offset: Point) extends Component {

  override def alpha = 1.0

  override def width: Int = child.width

  override def height: Int = child.height

  override def visible: Boolean = true

  override def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode] = js.Array()

  override val children: js.Array[Component] = js.Array(child)

  override def registerEvents(parentRectangle: Rectangle) = js.Array()

  override def anchor: Anchor = Anchor.topLeft.withOffset(offset)

}
