package game

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import models.bff.ingame.{Controls, KeyboardControls, UserInput}
import org.scalajs.dom
import typings.std.KeyboardEvent

import scala.scalajs.js

/**
  * Facility methods for having access to keyboard event.
  *
  * Note: each instance of this class will add an event listener to the document.
  */
final class Keyboard(controls: Controls) {

  private val downKeyEventBus: EventBus[KeyboardEvent] = new EventBus[KeyboardEvent]
  private val upKeyEventBus: EventBus[KeyboardEvent]   = new EventBus[KeyboardEvent]

  /** Stream of key-press events */
  val $downKeyEvents: EventStream[KeyboardEvent] = downKeyEventBus.events

  /** Stream of key-up events */
  val $upKeyEvents: EventStream[KeyboardEvent] = upKeyEventBus.events

  /** Merged key-press and key-up events. */
  val $keyboardEvents: EventStream[KeyboardEvent] = EventStream.merge($downKeyEvents, $upKeyEvents)

  /** Signal of all currently pressed key codes. */
  val $pressedKeys: Signal[Set[String]] = $keyboardEvents.fold(Set.empty[String]) {
    case (accumulatedSet, event) if event.`type` == "keyup"    => accumulatedSet - event.code
    case (accumulatedSet, event) if event.`type` == "keypress" => accumulatedSet + event.code
    case (s, _)                                                => s // should never happen as we don't register it
  }

  val downUserInputEvents = $keyboardEvents.filter(_.`type` == "keydown")
    .map(_.code)
    .map(Controls.KeyCode)
    .map(controls.getOrUnknown)
  val upUserInputEvents = $keyboardEvents.filter(_.`type` == "keyup")
    .map(_.code)
    .map(Controls.KeyCode)
    .map(controls.getOrUnknown)

  private val keyDownHandler: js.Function1[dom.KeyboardEvent, _] = (event: dom.KeyboardEvent) => {
    event.stopPropagation()
    event.preventDefault()
    downKeyEventBus.writer.onNext(event.asInstanceOf[KeyboardEvent])
  }
  private val keyUpHandler: js.Function1[dom.KeyboardEvent, _] = (event: dom.KeyboardEvent) => {
    event.stopPropagation()
    event.preventDefault()
    upKeyEventBus.writer.onNext(event.asInstanceOf[KeyboardEvent])
  }

  dom.document.addEventListener("keydown", keyDownHandler)
  dom.document.addEventListener("keyup", keyUpHandler)

  /** Removes the handlers on the document. This class will not work anymore after calling this. */
  def destroy(): Unit = {
    dom.document.removeEventListener("keydown", keyDownHandler)
    dom.document.removeEventListener("keyup", keyUpHandler)
  }

}
