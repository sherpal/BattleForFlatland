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
      parentRectangle: Rectangle,
      parentAlpha: Double
  ): js.Array[Component.ComponentWithItsBounds] =
    val thisWithItsBounds = withItsBounds(parentRectangle, parentAlpha)
    thisWithItsBounds +: children.flatMap(
      _.allDescendants(thisWithItsBounds.bounds, thisWithItsBounds.alpha)
    )

  def anchor: Anchor

  def withItsBounds(parentRectangle: Rectangle, parentAlpha: Double) =
    Component.ComponentWithItsBounds(this, positionRectangle(parentRectangle), alpha * parentAlpha)

  def width: Int
  def height: Int

  def alpha: Double

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

  def present(bounds: Rectangle, alpha: Double): js.Array[SceneNode]

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

  def presentWithChildren(parentRectangle: Rectangle, parentAlpha: Double): js.Array[SceneNode] =
    if visible then
      val myRectangle = positionRectangle(parentRectangle)
      val myAlpha     = alpha * parentAlpha
      present(myRectangle, myAlpha) ++ children.flatMap(
        _.presentWithChildren(myRectangle, myAlpha)
      )
    else js.Array()

  /** Same as [[presentWithChildren]], but assumes that you are going to place your component in
    * absolute position.
    */
  def presentWithChildrenWithoutRectangle: js.Array[SceneNode] =
    presentWithChildren(Rectangle(Size(0)), 1.0)

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
      bounds: Rectangle,
      alpha: Double
  ) {
    def registerEvents: js.Array[Component.EventRegistration[?]] =
      component.registerEvents(bounds)
  }

  case class CachedComponentsInfo(
      registeredEvents: js.Array[EventRegistration[?]],
      components: js.Array[ComponentWithItsBounds]
  ) {
    def presentAll: js.Array[SceneNode] =
      components.flatMap(comp => comp.component.present(comp.bounds, comp.alpha))

    def handleEvent(event: GlobalEvent): js.Array[GlobalEvent] =
      registeredEvents.flatMap(_.handle(event))
  }

  object CachedComponentsInfo {
    def empty: CachedComponentsInfo =
      CachedComponentsInfo(js.Array(), js.Array())
  }

}
