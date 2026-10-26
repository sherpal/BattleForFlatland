package bffclient.love

import models.bff.ingame.Controls.{InputCode, KeyCode, KeyInputModifier, ModifiedKeyCode, MouseCode}

/** Conversions between LÖVE's inputs and Battle for Flatland's [[models.bff.ingame.Controls]].
  *
  * BFF's controls use the DOM `KeyboardEvent.code` names ("KeyW", "Digit1", "Tab", ...), which
  * identify physical keys. LÖVE's scancodes identify physical keys too, so we map scancodes.
  */
object LoveInput {

  /** LÖVE scancode -> DOM `KeyboardEvent.code` (only codes known by `Controls`). */
  val scancodeToCode: Map[String, String] = {
    val letters = ('a' to 'z').map(c => c.toString -> s"Key${c.toUpper}")
    val digits  = ('0' to '9').map(c => c.toString -> s"Digit$c")
    val fKeys   = (1 to 24).map(i => s"f$i" -> s"F$i")
    val numpad  = ('0' to '9').map(c => s"kp$c" -> s"Numpad$c")
    (letters ++ digits ++ fKeys ++ numpad).toMap ++ Map(
      "return"         -> "Enter",
      "escape"         -> "Escape",
      "backspace"      -> "Backspace",
      "tab"            -> "Tab",
      "space"          -> "Space",
      "-"              -> "Minus",
      "="              -> "Equal",
      "["              -> "BracketLeft",
      "]"              -> "BracketRight",
      "\\"             -> "Backslash",
      ";"              -> "Semicolon",
      "'"              -> "Quote",
      "`"              -> "Backquote",
      ","              -> "Comma",
      "."              -> "Period",
      "/"              -> "Slash",
      "capslock"       -> "CapsLock",
      "printscreen"    -> "PrintScreen",
      "scrolllock"     -> "ScrollLock",
      "pause"          -> "Pause",
      "insert"         -> "Insert",
      "home"           -> "Home",
      "pageup"         -> "PageUp",
      "delete"         -> "Delete",
      "end"            -> "End",
      "pagedown"       -> "PageDown",
      "up"             -> "ArrowUp",
      "left"           -> "ArrowLeft",
      "down"           -> "ArrowDown",
      "right"          -> "ArrowRight",
      "numlock"        -> "NumLock",
      "kp/"            -> "NumpadDivide",
      "kp*"            -> "NumpadMultiply",
      "kp-"            -> "NumpadSubtract",
      "kp+"            -> "NumpadAdd",
      "kpenter"        -> "NumpadEnter",
      "kp."            -> "NumpadDecimal",
      "nonusbackslash" -> "IntlBackslash",
      "application"    -> "ContextMenu",
      "lshift"         -> "ShiftLeft",
      "rshift"         -> "ShiftRight",
      "lctrl"          -> "ControlLeft",
      "rctrl"          -> "ControlRight",
      "lalt"           -> "AltLeft",
      "ralt"           -> "AltRight",
      "lgui"           -> "MetaLeft",
      "rgui"           -> "MetaRight"
    )
  }

  val codeToScancode: Map[String, String] = scancodeToCode.map(_.swap)

  def shiftDown: Boolean = Keyboard.isScancodeDown("lshift") || Keyboard.isScancodeDown("rshift")

  /** The input code of a key event, like the frontend's `KeyboardHandler.enrich`. */
  def inputCode(scancode: String): Option[InputCode] =
    scancodeToCode.get(scancode).map { code =>
      if shiftDown then ModifiedKeyCode(code, KeyInputModifier.WithShift) else KeyCode(code)
    }

  /** Whether the key (or mouse button) of this input code is currently held. */
  def isDown(inputCode: InputCode): Boolean = inputCode match {
    case KeyCode(code) => codeToScancode.get(code).exists(Keyboard.isScancodeDown)
    case ModifiedKeyCode(code, KeyInputModifier.WithShift) =>
      shiftDown && codeToScancode.get(code).exists(Keyboard.isScancodeDown)
    case MouseCode(button) => Mouse.isDown(loveButton(button))
  }

  /** Mouse buttons are numbered like the DOM's `MouseEvent.button` in BFF's controls (0 left, 1
    * middle, 2 right, 3 and 4 the side buttons), and from 1 in LÖVE (1 left, 2 right, 3 middle,
    * ...).
    */
  def domButton(loveButton: Int): Int = loveButton match {
    case 1 => 0
    case 2 => 2
    case 3 => 1
    case other => other - 1
  }

  def loveButton(domButton: Int): Int = domButton match {
    case 0 => 1
    case 1 => 3
    case 2 => 2
    case other => other + 1
  }

  /** The input code of a mouse button (LÖVE's number). */
  def mouseCode(loveButton: Int): InputCode = MouseCode(domButton(loveButton))
}
