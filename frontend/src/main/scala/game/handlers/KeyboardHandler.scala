package game.handlers

import indigo.*
import models.bff.ingame.Controls.*
import models.bff.ingame.Controls.KeyInputModifier.WithShift
import models.bff.ingame.Controls
import models.bff.ingame.UserInput
import game.scenes.ingame.InGameScene.StartupData

trait KeyboardHandler {

  extension [Kbd <: KeyboardEvent](event: Kbd) {
    def enrich(context: FrameContext[StartupData]) = KeyboardHandler.enrich(context, event)
  }

}

object KeyboardHandler {
  case class RichKeyboardEvent[Kbd <: KeyboardEvent](
      kbd: Kbd,
      maybeInputCode: Option[InputCode],
      userInput: UserInput
  ) {

    inline def collectUserInput[T](pf: PartialFunction[UserInput, T]): Option[T] =
      pf.lift(userInput)

    inline def isUserInput(input: UserInput): Boolean = userInput == input

  }

  def enrich[Kbd <: KeyboardEvent](
      context: FrameContext[StartupData],
      kbd: Kbd
  ): RichKeyboardEvent[Kbd] =
    val maybeInputCode = Controls.keyToKeyCodeMap
      .get(kbd.keyCode.code)
      .map { key =>
        if context.keyboard.keysDown.contains[Key](Key.SHIFT) then
          ModifiedKeyCode(key, KeyInputModifier.WithShift)
        else KeyCode(key)
      }
    RichKeyboardEvent(
      kbd,
      maybeInputCode,
      maybeInputCode.fold(UserInput.Unknown(None))(context.startUpData.controls.getOrUnknown)
    )
}
