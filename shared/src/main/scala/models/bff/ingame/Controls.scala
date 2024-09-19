package models.bff.ingame

import models.bff.ingame.Controls.InputCode
import models.syntax.Pointed
import gamelogic.gameextras.GameMarker
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import services.localstorage.LocalStorage

/** Gathers all possible inputs that the user assigned. The [[InputCode]]s in argument have the
  * knowledge of what device is the source of the input.
  *
  * @param markerOnTargetKeys
  *   Map from the [[InputCode]] to the marker that will be put on current target
  * @param markerOnPositionKeys
  *   Map from the [[InputCode]] to the marker that will be put on the current mouse position.
  */
final case class Controls(
    upKey: InputCode,
    downKey: InputCode,
    leftKey: InputCode,
    rightKey: InputCode,
    nextTargetKey: InputCode,
    abilityKeys: List[InputCode],
    gameMarkerControls: GameMarkerControls
) {

  lazy val controlMap: Map[InputCode, UserInput] = Map(
    upKey         -> UserInput.Up,
    downKey       -> UserInput.Down,
    leftKey       -> UserInput.Left,
    rightKey      -> UserInput.Right,
    nextTargetKey -> UserInput.NextTarget
  ) ++ abilityKeys.zipWithIndex.map { case (code, idx) =>
    code -> UserInput.AbilityInput(idx)
  }.toMap ++
    gameMarkerControls.controlMap

  def allKeysInMultiple: List[InputCode] =
    (List(upKey, downKey, rightKey, leftKey, nextTargetKey) ++ abilityKeys)
      .groupBy(identity)
      .filter(_._2.length > 1)
      .keys
      .toList

  /** Maybe returns a control key which is assigned twice. */
  def maybeMultipleKey: Option[InputCode] = allKeysInMultiple.headOption

  /** Retrieve the [[UserInput]] for this keyCode, or the [[UserInput.Unknown]] if it is not
    * defined.
    */
  def getOrUnknown(keyCode: InputCode): UserInput =
    controlMap.getOrElse(keyCode, UserInput.Unknown(keyCode))
}

object Controls {

  sealed trait InputSource {
    type Code
  }
  case object MouseSource extends InputSource {
    type Code = Int
  }
  case object KeyboardSource extends InputSource {
    type Code = String
  }

  /** Represent an operation when the user gives input via one of the sources. */
  sealed trait InputCode {
    val source: InputSource
    def code: source.Code
    def label: String
  }

  sealed trait KeyInputModifier {
    def name: String
  }
  object KeyInputModifier {
    case object WithShift extends KeyInputModifier {
      def name: String = "Shift"
    }
  }
  case class KeyCode(code: String) extends InputCode {
    val source: KeyboardSource.type = KeyboardSource
    def label                       = code

    val keyCode: Int = keyCodeToKeyMap(code)
  }
  case class ModifiedKeyCode(code: String, modifier: KeyInputModifier) extends InputCode {
    val source: KeyboardSource.type = KeyboardSource
    def label                       = s"${modifier.name} ${code}"

    val keyCode: Int = keyCodeToKeyMap(code)
  }
  case class MouseCode(code: Int) extends InputCode {
    val source: MouseSource.type = MouseSource
    def label                    = s"Button $code"
  }

  given Pointed[Controls] = Pointed.factory(
    Controls(
      KeyCode("KeyW"),
      KeyCode("KeyS"),
      KeyCode("KeyA"),
      KeyCode("KeyD"),
      KeyCode("Tab"),
      (1 to 10).map(_ % 10).map("Digit" + _).map(KeyCode(_)).toList,
      Pointed[GameMarkerControls].unit
    )
  )

  val storageKey = LocalStorage.key[Controls]("controls")

  private given Encoder[KeyInputModifier] = deriveEncoder
  private given Decoder[KeyInputModifier] = deriveDecoder

  private given Encoder[InputCode] = deriveEncoder
  private given Decoder[InputCode] = deriveDecoder

  private given Encoder[GameMarkerControls] = deriveEncoder
  private given Decoder[GameMarkerControls] = deriveDecoder

  given Encoder[Controls] = deriveEncoder
  given Decoder[Controls] = deriveDecoder

  def keyCodeToKeyMap = Map(
    "KeyA"           -> 65,
    "KeyB"           -> 66,
    "KeyC"           -> 67,
    "KeyD"           -> 68,
    "KeyE"           -> 69,
    "KeyF"           -> 70,
    "KeyG"           -> 71,
    "KeyH"           -> 72,
    "KeyI"           -> 73,
    "KeyJ"           -> 74,
    "KeyK"           -> 75,
    "KeyL"           -> 76,
    "KeyM"           -> 77,
    "KeyN"           -> 78,
    "KeyO"           -> 79,
    "KeyP"           -> 80,
    "KeyQ"           -> 81,
    "KeyR"           -> 82,
    "KeyS"           -> 83,
    "KeyT"           -> 84,
    "KeyU"           -> 85,
    "KeyV"           -> 86,
    "KeyW"           -> 87,
    "KeyX"           -> 88,
    "KeyY"           -> 89,
    "KeyZ"           -> 90,
    "Digit1"         -> 49,
    "Digit2"         -> 50,
    "Digit3"         -> 51,
    "Digit4"         -> 52,
    "Digit5"         -> 53,
    "Digit6"         -> 54,
    "Digit7"         -> 55,
    "Digit8"         -> 56,
    "Digit9"         -> 57,
    "Digit0"         -> 48,
    "Enter"          -> 13,
    "Escape"         -> 27,
    "Backspace"      -> 8,
    "Tab"            -> 9,
    "Space"          -> 32,
    "Minus"          -> 189,
    "Equal"          -> 187,
    "BracketLeft"    -> 219,
    "BracketRight"   -> 221,
    "Backslash"      -> 220,
    "Semicolon"      -> 186,
    "Quote"          -> 222,
    "Backquote"      -> 192,
    "Comma"          -> 188,
    "Period"         -> 190,
    "Slash"          -> 191,
    "CapsLock"       -> 20,
    "F1"             -> 112,
    "F2"             -> 113,
    "F3"             -> 114,
    "F4"             -> 115,
    "F5"             -> 116,
    "F6"             -> 117,
    "F7"             -> 118,
    "F8"             -> 119,
    "F9"             -> 120,
    "F10"            -> 121,
    "F11"            -> 122,
    "F12"            -> 123,
    "PrintScreen"    -> 44,
    "ScrollLock"     -> 145,
    "Pause"          -> 19,
    "Insert"         -> 45,
    "Home"           -> 36,
    "PageUp"         -> 33,
    "Delete"         -> 46,
    "End"            -> 35,
    "PageDown"       -> 34,
    "ArrowUp"        -> 38,
    "ArrowLeft"      -> 37,
    "ArrowDown"      -> 40,
    "ArrowRight"     -> 39,
    "NumLock"        -> 144,
    "NumpadDivide"   -> 111,
    "NumpadMultiply" -> 106,
    "NumpadSubtract" -> 109,
    "NumpadAdd"      -> 107,
    "NumpadEnter"    -> 13,
    "Numpad1"        -> 97,
    "Numpad2"        -> 98,
    "Numpad3"        -> 99,
    "Numpad4"        -> 100,
    "Numpad5"        -> 101,
    "Numpad6"        -> 102,
    "Numpad7"        -> 103,
    "Numpad8"        -> 104,
    "Numpad9"        -> 105,
    "Numpad0"        -> 96,
    "NumpadDecimal"  -> 110,
    "IntlBackslash"  -> 226,
    "ContextMenu"    -> 93,
    "ShiftLeft"      -> 16,
    "ShiftRight"     -> 16,
    "ControlLeft"    -> 17,
    "ControlRight"   -> 17,
    "AltLeft"        -> 18,
    "AltRight"       -> 18,
    "MetaLeft"       -> 91,
    "MetaRight"      -> 92,
    "OSLeft"         -> 91,
    "OSRight"        -> 92,
    "ContextMenu"    -> 93,
    "F13"            -> 124,
    "F14"            -> 125,
    "F15"            -> 126,
    "F16"            -> 127,
    "F17"            -> 128,
    "F18"            -> 129,
    "F19"            -> 130,
    "F20"            -> 131,
    "F21"            -> 132,
    "F22"            -> 133,
    "F23"            -> 134,
    "F24"            -> 135
  )

}
