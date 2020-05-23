package game

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import gamelogic.physics.Complex
import org.scalajs.dom.html
import typings.std.MouseEvent

import scala.scalajs.js

/**
  * Facility methods for accessing mouse events inside the given canvas.
  *
  * Constructing this class will add event listeners to the canvas.
  * Calling the `destroy` method removes these listeners and make this Mouse instance no more usable.
  */
final class Mouse(canvas: html.Canvas) {

  private def effectiveMousePos(clientX: Double, clientY: Double): Complex = {
    val boundingRect = canvas.getBoundingClientRect()
    Complex(clientX - boundingRect.left - canvas.width / 2, -clientY + boundingRect.top + canvas.height / 2)
  }

  def effectiveMousePos(event: MouseEvent): Complex =
    effectiveMousePos(event.clientX, event.clientY)

  private val mouseClickBus: EventBus[MouseEvent] = new EventBus
  val $mouseClicks: EventStream[MouseEvent]       = mouseClickBus.events

  private val clickHandler: js.Function1[MouseEvent, _] = { event: MouseEvent =>
    mouseClickBus.writer.onNext(event)
  }
  canvas.addEventListener("click", clickHandler)

  private val mouseMoveBus: EventBus[MouseEvent] = new EventBus
  val $mouseMove: EventStream[MouseEvent]        = mouseMoveBus.events

  val $effectiveMousePosition: EventStream[Complex] =
    $mouseMove.map(event => effectiveMousePos(event.clientX, event.clientY))

  private val mouseMoveHandler: js.Function1[MouseEvent, _] = { event: MouseEvent =>
    mouseMoveBus.writer.onNext(event)
  }
  canvas.addEventListener("mousemove", mouseMoveHandler)

}
