package game

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import gamelogic.physics.Complex
import models.bff.ingame.Controls
import org.scalajs.dom.html
import typings.std.MouseEvent

import scala.scalajs.js

/**
  * Facility methods for accessing mouse events inside the given canvas.
  *
  * Constructing this class will add event listeners to the canvas.
  * Calling the `destroy` method removes these listeners and make this Mouse instance no more usable.
  */
final class Mouse(canvas: html.Canvas, controls: Controls) {

  private def effectiveMousePos(clientX: Double, clientY: Double): Complex = {
    val boundingRect = canvas.getBoundingClientRect()
    Complex(clientX - boundingRect.left - canvas.width / 2, -clientY + boundingRect.top + canvas.height / 2)
  }

  /** Returns the [[gamelogic.physics.Complex]] mouse position corresponding to this [[typings.std.MouseEvent]]. */
  def effectiveMousePos(event: MouseEvent): Complex =
    effectiveMousePos(event.clientX, event.clientY)

  private val mouseClickBus: EventBus[MouseEvent] = new EventBus
  val $mouseClicks: EventStream[MouseEvent]       = mouseClickBus.events

  private val clickHandler: js.Function1[MouseEvent, _] = mouseClickBus.writer.onNext _
  private val contextMenuHandler: js.Function1[MouseEvent, _] = { (event: MouseEvent) =>
    mouseClickBus.writer.onNext(event)
    event.preventDefault()
  }

  canvas.addEventListener("click", clickHandler)
  canvas.addEventListener("contextmenu", contextMenuHandler)

  private val mouseDownEventBus = new EventBus[MouseEvent]
  private val mouseUpEventBus   = new EventBus[MouseEvent]

  private val mouseDownHandler: js.Function1[MouseEvent, _] = mouseDownEventBus.writer.onNext _
  private val mouseUpHandler: js.Function1[MouseEvent, _]   = mouseUpEventBus.writer.onNext _

  canvas.addEventListener("mouseup", mouseUpHandler)
  canvas.addEventListener("mousedown", mouseDownHandler)

  val downUserInputEvents = mouseDownEventBus.events
    .map(_.button.toInt)
    .map(Controls.MouseCode)
    .map(controls.getOrUnknown)
  val upUserInputEvents = mouseUpEventBus.events
    .map(_.button.toInt)
    .map(Controls.MouseCode)
    .map(controls.getOrUnknown)

  private val mouseMoveBus: EventBus[MouseEvent] = new EventBus
  val $mouseMove: EventStream[MouseEvent]        = mouseMoveBus.events

  val $effectiveMousePosition: EventStream[Complex] =
    $mouseMove.map(event => effectiveMousePos(event.clientX, event.clientY))

  private val mouseMoveHandler: js.Function1[MouseEvent, _] = { event: MouseEvent =>
    mouseMoveBus.writer.onNext(event)
  }
  canvas.addEventListener("mousemove", mouseMoveHandler)

}
