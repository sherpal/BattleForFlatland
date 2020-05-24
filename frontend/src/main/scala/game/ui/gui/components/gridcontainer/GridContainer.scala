package game.ui.gui.components.gridcontainer

import game.ui.gui.components.GUIComponent
import game.ui.gui.components.gridcontainer.GridContainer.GridDirection
import gamelogic.gamestate.GameState

import Ordering.Double.TotalOrdering
import scala.scalajs.js

/**
  * A grid container is used to display a list of pixi containers
  */
final class GridContainer[T <: GUIComponent](
    direction: GridDirection,
    nbrElementsInFirstDirection: Int,
    nbrElementsInSecondDirection: Int
)(implicit ordering: Ordering[T])
    extends GUIComponent {

  private var elements: Vector[T] = Vector()

  def currentElements: Vector[T] = elements

  def filter(predicate: T => Boolean): Unit = {
    val (newElements, removedElements) = elements.partition(predicate)
    removedElements.map(_.container).foreach(container.removeChild)
    elements = newElements
    display()
  }

  def isEmpty: Boolean = elements.isEmpty

  def addElement(t: T): Unit = {
    elements = t +: elements
    container.addChild(t.container)
    display()
  }
  def addElements(ts: Iterable[T]): Unit = {
    elements = elements ++ ts
    ts.map(_.container).foreach(container.addChild)
    display()
  }

  def removeElement(t: T): Unit = {
    elements = elements.filterNot(_ == t)
    container.removeChild(t.container)
    display()
  }

  private def elementsByLine =
    currentElements.sorted
      .take(nbrElementsInFirstDirection * nbrElementsInSecondDirection)
      .grouped(nbrElementsInFirstDirection)
      .toVector

  private def displayLine(line: Vector[T], startCoordinate: Double): Double = {
    val coordinateForNextLine = startCoordinate + line
      .map(t => if (direction == GridContainer.Row) t.container.width else t.container.height)
      .maxOption
      .getOrElse(0.0)

    if (direction == GridContainer.Row) {
      line.foldLeft(0.0) { (coordinate, element) =>
        element.container.y = coordinate
        element.container.x = startCoordinate
        coordinate + element.container.height
      }
    } else {
      line.foldLeft(0.0) { (coordinate, element) =>
        element.container.x = coordinate
        element.container.y = startCoordinate
        coordinate + element.container.width
      }
    }

    coordinateForNextLine
  }

  def display(): Unit =
    elementsByLine.foldLeft(0.0) { (coordinate, line) =>
      displayLine(line, coordinate)
    }

  def update(gameState: GameState, currentTime: Long): Unit = {
    display()
    currentElements.foreach(_.update(gameState, currentTime))
  }

}

object GridContainer {

  sealed trait GridDirection
  case object Row extends GridDirection // first filling the first column, then the second...
  case object Column extends GridDirection // first filling the first row, then the second row, etc.

}
