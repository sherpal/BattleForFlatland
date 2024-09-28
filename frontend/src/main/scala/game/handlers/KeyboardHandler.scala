package game.handlers

import models.bff.ingame.Controls.InputCode
import indigo.*
import models.bff.ingame.Controls.InputCode
import models.bff.ingame.Controls.KeyCode
import models.bff.ingame.Controls.ModifiedKeyCode
import models.bff.ingame.Controls.MouseCode
import models.bff.ingame.Controls.KeyInputModifier.WithShift
import models.bff.ingame.Controls
import models.bff.ingame.Controls.KeyInputModifier

trait KeyboardHandler {

  extension (event: KeyboardEvent) {
    def toInputCode(downKeys: Batch[Key]): Option[InputCode] =
      Controls.keyToKeyCodeMap.get(event.keyCode.code).map { key =>
        if downKeys.contains[Key](Key.SHIFT) then ModifiedKeyCode(key, KeyInputModifier.WithShift)
        else KeyCode(key)
      }
  }

  protected def wasInputCode(
      inputCode: InputCode,
      event: KeyboardEvent,
      downKeys: Batch[Key]
  ): Boolean = inputCode match
    case kc: KeyCode =>
      kc.keyCode == event.keyCode.code && !downKeys.contains[Key](Key.SHIFT)
    case mkc @ ModifiedKeyCode(_, modifier) =>
      def isModifierDown = modifier match
        case WithShift => downKeys.contains[Key](Key.SHIFT)

      mkc.keyCode == event.keyCode.code && isModifierDown
    case MouseCode(_) => false

}
