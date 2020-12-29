package models.bff.ingame

import models.bff.ingame.Controls._
import models.syntax.Pointed
import gamelogic.gameextras.GameMarker

final case class GameMarkerControls(
  crossTargetKey: InputCode,
  lozengeTargetKey: InputCode,
  moonTargetKey: InputCode,
  squareTargetKey: InputCode,
  starTargetKey: InputCode,
  triangleTargetKey: InputCode,
  crossFixedKey: InputCode,
  lozengeFixedKey: InputCode,
  moonFixedKey: InputCode,
  squareFixedKey: InputCode,
  starFixedKey: InputCode,
  triangleFixedKey: InputCode,
) {
  def controlMap: Map[InputCode, UserInput] = Map(
    crossTargetKey -> UserInput.GameMarkerInput(GameMarker.Cross, onTarget = true),
    lozengeTargetKey -> UserInput.GameMarkerInput(GameMarker.Lozenge, onTarget = true),
    moonTargetKey -> UserInput.GameMarkerInput(GameMarker.Moon, onTarget = true),
    squareTargetKey -> UserInput.GameMarkerInput(GameMarker.Square, onTarget = true),
    starTargetKey -> UserInput.GameMarkerInput(GameMarker.Star, onTarget = true),
    triangleTargetKey -> UserInput.GameMarkerInput(GameMarker.Triangle, onTarget = true),
    crossFixedKey -> UserInput.GameMarkerInput(GameMarker.Cross, onTarget = false),
    lozengeFixedKey -> UserInput.GameMarkerInput(GameMarker.Lozenge, onTarget = false),
    moonFixedKey -> UserInput.GameMarkerInput(GameMarker.Moon, onTarget = false),
    squareFixedKey -> UserInput.GameMarkerInput(GameMarker.Square, onTarget = false),
    starFixedKey -> UserInput.GameMarkerInput(GameMarker.Star, onTarget = false),
    triangleFixedKey -> UserInput.GameMarkerInput(GameMarker.Triangle, onTarget = false),
  )
}

object GameMarkerControls {

  implicit val pointed: Pointed[GameMarkerControls] = Pointed.factory(GameMarkerControls(
    KeyCode("KeyR"),
    KeyCode("KeyF"),
    KeyCode("KeyV"),
    KeyCode("KeyT"),
    KeyCode("KeyG"),
    KeyCode("KeyB"),
    ModifiedKeyCode("KeyR", KeyInputModifier.WithShift),
    ModifiedKeyCode("KeyF", KeyInputModifier.WithShift),
    ModifiedKeyCode("KeyV", KeyInputModifier.WithShift),
    ModifiedKeyCode("KeyT", KeyInputModifier.WithShift),
    ModifiedKeyCode("KeyG", KeyInputModifier.WithShift),
    ModifiedKeyCode("KeyB", KeyInputModifier.WithShift)
  ))

}

/**
  * private val gameMarkerMap = Map(
    KeyCode("KeyR") -> GameMarker.Cross,
    KeyCode("KeyF") -> GameMarker.Lozenge,
    KeyCode("KeyV") -> GameMarker.Moon,
    KeyCode("KeyT") -> GameMarker.Square,
    KeyCode("KeyG") -> GameMarker.Star,
    KeyCode("KeyB") -> GameMarker.Triangle
  )
  */