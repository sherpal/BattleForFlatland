package game.ui.components.grid

import indigo.*
import game.ui.*

import scala.scalajs.js
import game.ui.components.grid.GridContainer.GridDirection
import game.ui.components.grid.GridContainer.Row
import game.ui.components.grid.GridContainer.Column

final case class GridContainer(
    direction: GridDirection,
    numberOfElementsInGridDirection: Int,
    cellChildren: js.Array[Component],
    anchor: Anchor,
    visible: Boolean = true
) extends Component {

  val cellWidth  = cellChildren.map(_.width).maxOption.getOrElse(0)
  val cellHeight = cellChildren.map(_.height).maxOption.getOrElse(0)

  val numberOfCrossDirectionLines =
    cellChildren.length / numberOfElementsInGridDirection + (if cellChildren.length % numberOfElementsInGridDirection == 0
                                                             then 0
                                                             else 1)

  val width: Int = direction match
    case Row    => numberOfElementsInGridDirection.min(cellChildren.length) * cellWidth
    case Column => numberOfCrossDirectionLines * cellWidth

  val height: Int = direction match
    case Row    => numberOfCrossDirectionLines * cellWidth
    case Column => numberOfElementsInGridDirection.min(cellChildren.length) * cellHeight

  lazy val children: js.Array[Component] = {
    val childrenArr = new js.Array[Component](cellChildren.length)
    for {
      dirIndex   <- 0 until numberOfElementsInGridDirection
      crossIndex <- 0 until numberOfCrossDirectionLines
      childIndex = crossIndex * numberOfElementsInGridDirection + dirIndex
      if childIndex < cellChildren.length
    } {
      val offset = direction match
        case Row =>
          Point(
            dirIndex * cellWidth,
            crossIndex * cellHeight
          )
        case Column =>
          Point(
            crossIndex * cellWidth,
            dirIndex * cellHeight
          )

      childrenArr(childIndex) = GridCell(cellChildren(childIndex), offset)
    }

    childrenArr
  }

  override def registerEvents(parentRectangle: Rectangle) = js.Array()

  override def present(bounds: Rectangle): js.Array[SceneNode] = js.Array()

}

object GridContainer {

  sealed trait GridDirection
  case object Row    extends GridDirection // first filling the first column, then the second...
  case object Column extends GridDirection // first filling the first row, then the second row, etc.

}
