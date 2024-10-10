package game.handlers

import indigo.*
import models.bff.ingame.UserInput
import game.events.CustomIndigoEvents

import scala.scalajs.js

object ToggleLockInTargetHandler {

  def handleKeyboardEvent(
      event: KeyboardHandler.RichKeyboardEvent[KeyboardEvent.KeyUp]
  ): js.Array[GlobalEvent] =
    if event.isUserInput(UserInput.ToggleTargetLockIn) then
      js.Array(CustomIndigoEvents.GameEvent.ToggleTargetLockIn())
    else js.Array()
}
