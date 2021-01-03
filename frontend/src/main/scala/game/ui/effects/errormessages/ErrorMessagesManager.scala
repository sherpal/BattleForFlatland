package game.ui.effects.errormessages

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.core.Observer
import com.raquo.airstream.signal.Signal
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner

import game.ui.reactivepixi.ReactivePixiElement._
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.PixiModifier
import typings.pixiJs.anon.Align
import typings.pixiJs.mod.TextStyle

import scala.scalajs.js

/**
  * The [[ErrorMessagesManager]] is responsible for furnishing a PIXI container that
  * displays the error messages that happen in the game.
  *
  * See the companion object to discover what is actually exposed to the world. In particular,
  * the outside world is responsible to
  * - tell when a new message arrives
  * - tell to update
  * - use the provided PIXI container and add it to the global scene.
  *
  * This is probably one of the ugliest code within the project, so please help improve it.
  */
private[errormessages] final class ErrorMessagesManager {

  implicit val owner = new Owner {}

  private def now = System.currentTimeMillis()

  private val eventEventBus: EventBus[Event] = new EventBus
  private val updateEventBus: EventBus[Long] = new EventBus

  val messageObserver: Observer[String] = eventEventBus.writer.contramap[String](
    str => Event.NewMessage(str, now)
  )

  val updateObserver: Observer[Long] = updateEventBus.writer

  private val stateSignal: Signal[ErrorMessageState] = eventEventBus.events.fold(ErrorMessageState.empty) {
    (state, event) =>
      event.act(state)
  }

  private val messageStates = updateEventBus.events.withCurrentValueOf(stateSignal).map {
    case (time, state) => state.messageStatesNow(time)
  }

  messageStates.map(_.filter(_._2 <= 0)).filter(_.nonEmpty).delay().foreach {
    (endedMessages: List[(MessageInfo, Double)]) =>
      endedMessages.foreach { case (message, _) => eventEventBus.writer.onNext(Event.MessageRemoved(message)) }
  }

  private val messagesToDisplay: EventStream[List[(String, Double)]] = messageStates
    .map(_.filter(_._2 > 0))
    .map(
      _.map { case (MessageInfo(message, _), alpha) => (message, alpha) }
    )
    .filter(_.nonEmpty)
    .map(_.reverse) // we need to reverse since before the head is the most recent element.

  def textN(idx: Int): PixiModifier[ReactiveContainer] = {
    val messages = messagesToDisplay.map(_.drop(idx).headOption).map {
      case Some(value) => value
      case None        => ("", 1.0)
    }
    pixiText(
      "",
      alpha <-- messages.map(_._2),
      text  <-- messages.map(_._1),
      anchorXY := (0.5, 0),
      y := (idx * 22 + 30),
      textStyle := new TextStyle(
        Align()
          .setFontSize(20)
          .setFillVarargs("#ff0000")
      )
    )
  }

  private val container = pixiContainer(
    List(0, 1, 2).map(textN(_)): _*
  )

}

object ErrorMessagesManager {

  private val manager = new ErrorMessagesManager

  /**
    * Spawns the following message at the top of the screen.
    * It disappear after some time, and only three such messages will be displayed
    * at the same time. If a fourth one arrives, the oldest is discarded.
    */
  def logError(message: String): Unit = manager.messageObserver.onNext(message)

  /**
    * Updates the error message view at the given time.
    *
    * Must be called, typically, 60 times per second.
    */
  def updateMessageView(time: Long): Unit = manager.updateObserver.onNext(time)

  /**
    * You can use this container inside the game scene.
    */
  val container: ReactiveContainer = manager.container

}
