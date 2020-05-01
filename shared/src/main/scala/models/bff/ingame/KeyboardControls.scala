package models.bff.ingame

import models.bff.ingame.KeyboardControls.KeyCode
import models.syntax.Pointed

final case class KeyboardControls(
    upKey: KeyCode,
    downKey: KeyCode,
    leftKey: KeyCode,
    rightKey: KeyCode
) {

  lazy val controlMap: Map[KeyCode, UserInput] = Map(
    upKey -> UserInput.Up,
    downKey -> UserInput.Down,
    leftKey -> UserInput.Left,
    rightKey -> UserInput.Right
  )

}

object KeyboardControls {

  type KeyCode = String

  implicit val pointed: Pointed[KeyboardControls] = Pointed.factory(
    KeyboardControls(
      "ArrowUp",
      "ArrowDown",
      "ArrowLeft",
      "ArrowRight"
    )
  )

}
