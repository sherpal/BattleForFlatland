package models.bff.ingame

import models.bff.ingame.KeyboardControls.KeyCode
import models.syntax.Pointed

final case class KeyboardControls(
    upKey: KeyCode,
    downKey: KeyCode,
    leftKey: KeyCode,
    rightKey: KeyCode,
    abilityKeys: List[KeyCode]
) {

  lazy val controlMap: Map[KeyCode, UserInput] = Map(
    upKey -> UserInput.Up,
    downKey -> UserInput.Down,
    leftKey -> UserInput.Left,
    rightKey -> UserInput.Right
  ) ++ abilityKeys.zipWithIndex.map { case (code, idx) => code -> UserInput.AbilityInput(idx) }.toMap

  /** Maybe returns a control key which is assigned twice. */
  def maybeMultipleKey: Option[KeyCode] =
    (List(upKey, downKey, rightKey, leftKey) ++ abilityKeys)
      .groupBy(identity)
      .find(_._2.length > 1)
      .map(_._1)

  /** Retrieve the [[UserInput]] for this keyCode, or the [[UserInput.Unknown]] if it is not defined. */
  def getOrUnknown(keyCode: KeyCode): UserInput =
    controlMap.getOrElse(keyCode, UserInput.Unknown(Controls.KeyCode(keyCode)))

}

object KeyboardControls {

  type KeyCode = String

  implicit val pointed: Pointed[KeyboardControls] = Pointed.factory(
    KeyboardControls(
      "KeyW",
      "KeyS",
      "KeyA",
      "KeyD",
      (1 to 10).map(_ % 10).map("Digit" + _).toList
    )
  )

  val storageKey = "controls"

}
