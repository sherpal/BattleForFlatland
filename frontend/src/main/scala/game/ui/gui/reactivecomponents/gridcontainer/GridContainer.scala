package game.ui.gui.reactivecomponents.gridcontainer

import com.raquo.airstream.signal.Signal
import game.ui.gui.reactivecomponents.GUIComponent
import game.ui.gui.reactivecomponents.gridcontainer.GridContainer.GridDirection
import game.ui.reactivepixi.ChildrenReceiver.children
import game.ui.reactivepixi.ReactivePixiElement

import scala.Ordering.Double.TotalOrdering

final class GridContainer[T <: ReactivePixiElement.ReactiveContainer](
    direction: GridDirection,
    gridChildrenSignal: Signal[List[T]],
    nbrElementsInFirstDirection: Int
) extends GUIComponent {

  private def displayLine(line: List[T], startCoordinate: Double): Double = {
    val coordinateForNextLine = startCoordinate + line
      .map(t => if (direction == GridContainer.Row) t.ref.width else t.ref.height)
      .maxOption
      .getOrElse(0.0)

    if (direction == GridContainer.Row) {
      line.foldLeft(0.0) { (coordinate, element) =>
        element.ref.y = coordinate
        element.ref.x = startCoordinate
        coordinate + element.ref.height
      }
    } else {
      line.foldLeft(0.0) { (coordinate, element) =>
        element.ref.x = coordinate
        element.ref.y = startCoordinate
        coordinate + element.ref.width
      }
    }

    coordinateForNextLine
  }

  container.amend(
    children <-- gridChildrenSignal.map { gridChildren =>
      gridChildren.grouped(nbrElementsInFirstDirection).foldLeft(0.0) { (coordinate, line) =>
        displayLine(line, coordinate)
      }

      gridChildren
    }
  )

}

object GridContainer {

  sealed trait GridDirection
  case object Row extends GridDirection // first filling the first column, then the second...
  case object Column extends GridDirection // first filling the first row, then the second row, etc.

}
