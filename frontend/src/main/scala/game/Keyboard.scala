package game

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import models.bff.ingame.{KeyboardControls, UserInput}
import typings.std.KeyboardEvent
import org.scalajs.dom

import scala.scalajs.js

/**
  * Facility methods for having access to keyboard event.
  *
  * Note: each instance of this class will add an event listener to the document.
  */
final class Keyboard(controls: KeyboardControls) {

  private val downKeyEventBus: EventBus[KeyboardEvent] = new EventBus[KeyboardEvent]
  private val upKeyEventBus: EventBus[KeyboardEvent]   = new EventBus[KeyboardEvent]

  /** Stream of key-press events */
  val $downKeyEvents: EventStream[KeyboardEvent] = downKeyEventBus.events

  /** Stream of key-up events */
  val $upKeyEvents: EventStream[KeyboardEvent] = upKeyEventBus.events

  /** Merged key-press and key-up events. */
  val $keyboardEvents: EventStream[KeyboardEvent] = EventStream.merge($downKeyEvents, $upKeyEvents)

  /** Signal of all currently pressed key codes. */
  val $pressedKeys: Signal[Set[String]] = $keyboardEvents.fold(Set[String]()) {
    case (accumulatedSet, event) if event.`type` == "keyup"    => accumulatedSet - event.code
    case (accumulatedSet, event) if event.`type` == "keypress" => accumulatedSet + event.code
    case (s, _)                                                => s // should never happen as we don't register it
  }

  /** Signal of all currently pressed [[models.bff.ingame.UserInput]] instances. */
  val $pressedUserInput: Signal[Set[UserInput]] = $keyboardEvents.fold(Set[UserInput]()) {
    case (accumulatedSet, event) =>
      val userInput = controls.controlMap.getOrElse(event.code, UserInput.Unknown(event.code))
      if (event.`type` == "keyup") accumulatedSet - userInput
      else if (event.`type` == "keydown") accumulatedSet + userInput
      else accumulatedSet // should never happen as we don't register it
  }

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
