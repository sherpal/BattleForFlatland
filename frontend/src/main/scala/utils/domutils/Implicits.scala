package utils.domutils

import typings.std.KeyboardEvent
import models.bff.ingame.Controls._

object Implicits {

  implicit class KeyboardEventEnhanced(keyboardEvent: KeyboardEvent) {
    def toInputCode: InputCode =
      if (keyboardEvent.shiftKey) ModifiedKeyCode(keyboardEvent.code, KeyInputModifier.WithShift)
      else KeyCode(keyboardEvent.code)
  }

}