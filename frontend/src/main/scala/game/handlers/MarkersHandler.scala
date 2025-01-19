package game.handlers

import indigo.*
import game.IndigoViewModel
import models.bff.ingame.Controls

import scala.scalajs.js
import game.scenes.ingame.InGameScene
import game.events.CustomIndigoEvents
import models.bff.ingame.UserInput
import gamelogic.gameextras.GameMarkerInfo

class MarkersHandler() {

  def handleKeyboardEvent(
      event: KeyboardHandler.RichKeyboardEvent[KeyboardEvent.KeyUp],
      context: FrameContext[InGameScene.StartupData],
      model: InGameScene.InGameModel,
      viewModel: IndigoViewModel,
      now: Long
  ): js.Array[CustomIndigoEvents] = (for {
    inputCode <- event.maybeInputCode
    userInput <- context.startUpData.controls.get(inputCode)
  } yield userInput).collect { case UserInput.GameMarkerInput(gameMarker, onTarget) =>
    (gameMarker, onTarget)
  } match {
    case None => js.Array()
    case Some((gameMarker, true)) =>
      viewModel.maybeTargetId match {
        case None => js.Array()
        case Some(targetId) =>
          js.Array(
            CustomIndigoEvents.GameEvent.PutMarkers(
              GameMarkerInfo.GameMarkerOnEntity(gameMarker, targetId)
            )
          )
      }
    case Some((gameMarker, false)) =>
      js.Array(
        CustomIndigoEvents.GameEvent.PutMarkers(
          GameMarkerInfo.FixedGameMarker(gameMarker, viewModel.worldMousePosition)
        )
      )
  }

}
