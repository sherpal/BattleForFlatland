package game

import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.signal.Signal
import gamelogic.physics.Complex
import models.bff.ingame.UserInput
import typings.std.MouseEvent

/**
  * This class combines all different input sources that the user can use.
  * Currently only Keyboard and Mouse are supported, but maybe in the future game controllers could be used as well.
  */
final class UserControls(keyboard: Keyboard, mouse: Mouse) {

  val downInputs = EventStream.merge(keyboard.downUserInputEvents, mouse.downUserInputEvents)
  val upInputs   = EventStream.merge(keyboard.upUserInputEvents, mouse.upUserInputEvents)

  val $pressedUserInput: Signal[Set[UserInput]] =
    EventStream
      .merge(
        downInputs.map(_ -> true),
        upInputs.map(_ -> false)
      )
      .fold(Set.empty[UserInput]) {
        case (accumulated, (input, isDown)) =>
          if (isDown) accumulated + input else accumulated - input
      }

  def $effectiveMousePosition: EventStream[Complex] = mouse.$effectiveMousePosition

  def $mouseClicks: EventStream[MouseEvent] = mouse.$mouseClicks

  def effectiveMousePos(event: MouseEvent): Complex = mouse.effectiveMousePos(event)

}
