package game.handlers

import indigo.*
import game.IndigoViewModel
import models.bff.ingame.Controls

import scala.scalajs.js
import game.scenes.ingame.InGameScene
import game.events.CustomIndigoEvents
import models.bff.ingame.UserInput
import gamelogic.gameextras.GameMarkerInfo

class MarkersHandler(controls: Controls) extends KeyboardHandler {

  def handleKeyboardEvent(
      event: KeyboardEvent.KeyUp,
      context: FrameContext[InGameScene.StartupData],
      model: InGameScene.InGameModel,
      viewModel: IndigoViewModel,
      now: Long
  ): js.Array[CustomIndigoEvents] = (for {
    inputCode <- event.toInputCode(context.keyboard.keysDown)
    userInput <- controls.get(inputCode)
  } yield userInput).collect { case UserInput.GameMarkerInput(gameMarker, onTarget) =>
    (gameMarker, onTarget)
  } match {
    case None => js.Array()
    case Some((gameMarker, true)) =>
      viewModel.maybeTarget match {
        case None => js.Array()
        case Some(target) =>
          js.Array(
            CustomIndigoEvents.GameEvent.PutMarkers(
              GameMarkerInfo.GameMarkerOnEntity(gameMarker, target.id)
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
