package game.ui

import indigo.*
import game.ui.AnchorPoint.*

import scala.reflect.Typeable

import scala.scalajs.js
import game.ui.Component.EventRegistration
import indigo.shared.events.MouseEvent.Click

trait Component {

  def children: js.Array[Component]

  def allDescendants(
      parentRectangle: Rectangle
  ): js.Array[Component.ComponentWithItsBounds] =
    val thisWithItsBounds = withItsBounds(parentRectangle)
    thisWithItsBounds +: children.flatMap(
      _.allDescendants(thisWithItsBounds.bounds)
    )

  def anchor: Anchor

  def withItsBounds(parentRectangle: Rectangle) =
    Component.ComponentWithItsBounds(this, positionRectangle(parentRectangle))

  def width: Int
  def height: Int

  def visible: Boolean

  def registerEvents(bounds: Rectangle): js.Array[Component.EventRegistration[?]]

  final def registerEventsWithChildren(
      parentRectangle: Rectangle
  ): js.Array[Component.EventRegistration[?]] =
    val myRectangle = positionRectangle(parentRectangle)
    val childrenEvents =
      if propagateToChildren then
        children.flatMap(
          _.registerEventsWithChildren(myRectangle)
        )
      else js.Array()
    registerEvents(myRectangle) ++ childrenEvents

  def present(bounds: Rectangle): js.Array[SceneNode]

  final def size = Size(width, height)

  final def positionRectangle(parentRectangle: Rectangle): Rectangle = {
    val theSize = size
    val topLeft = anchor.offset + anchor.point.pointRelativeTo(
      Rectangle(theSize)
    ) - anchor.relativePoint.pointRelativeTo(
      parentRectangle
    ) + parentRectangle.position

    Rectangle(topLeft, theSize)
  }

  def propagateToChildren: Boolean = true

  def presentWithChildren(parentRectangle: Rectangle): js.Array[SceneNode] = {
    val myRectangle = positionRectangle(parentRectangle)

    if visible then
      present(myRectangle) ++ children.flatMap(
        _.presentWithChildren(myRectangle)
      )
    else js.Array()
  }

  protected def registerClickInBounds(bounds: Rectangle)(
      events: => js.Array[GlobalEvent]
  ): EventRegistration[Click] =
    EventRegistration(click => if bounds.isPointWithin(click.position) then events else js.Array())

}

object Component {

  class EventRegistration[Ev <: GlobalEvent](f: Ev => js.Array[GlobalEvent])(using Typeable[Ev]) {
    def handle(event: GlobalEvent): js.Array[GlobalEvent] = event match {
      case ev: Ev => f(ev)
      case _      => js.Array()
    }
  }

  case class ComponentWithItsBounds(
      component: Component,
      bounds: Rectangle
  ) {
    def registerEvents: js.Array[Component.EventRegistration[?]] =
      component.registerEvents(bounds)
  }

  case class CachedComponentsInfo(
      registeredEvents: js.Array[EventRegistration[?]],
      components: js.Array[ComponentWithItsBounds]
  ) {
    def presentAll: js.Array[SceneNode] =
      components.flatMap(comp => comp.component.present(comp.bounds))

    def handleEvent(event: GlobalEvent): js.Array[GlobalEvent] =
      registeredEvents.flatMap(_.handle(event))
  }

  object CachedComponentsInfo {
    def empty: CachedComponentsInfo =
      CachedComponentsInfo(js.Array(), js.Array())
  }

}
