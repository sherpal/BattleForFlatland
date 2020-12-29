package game

import com.raquo.airstream.signal.Signal
import gamelogic.entities.MovingBody
import gamelogic.entities.LivingEntity
import com.raquo.airstream.core.Observer
import models.bff.ingame.InGameWSProtocol
import models.bff.ingame.UserInput
import gamelogic.gamestate.gameactions.markers.UpdateMarker
import gamelogic.gameextras.GameMarkerInfo
import gamelogic.physics.Complex
import com.raquo.airstream.eventstream.EventStream
import com.raquo.airstream.ownership.Owner

/**
  * Singleton managing players putting markers in the game.
  */
final class MarkerManager(
  userControls: UserControls,
  maybeTargetSignal: Signal[Option[MovingBody with LivingEntity]],
  gameMousePositionSignal: Signal[Complex],
  socketOutWriter: Observer[InGameWSProtocol.GameActionWrapper],
  currentTime: () => Long
)(implicit owner: Owner) {
  private def serverTime = currentTime()

  val gameMarkerInputs = userControls.downInputs.collect {
    case input: UserInput.GameMarkerInput => input
  }

  val onTargetMarkers = gameMarkerInputs.collect {
    case UserInput.GameMarkerInput(marker, true) => marker
  }.withCurrentValueOf(maybeTargetSignal)
  .collect {
    case (marker, Some(target)) => 
      GameMarkerInfo.GameMarkerOnEntity(marker, target.id): GameMarkerInfo
  }

  val fixedMarkers = gameMarkerInputs.collect {
    case UserInput.GameMarkerInput(marker, false) => marker
  }.withCurrentValueOf(gameMousePositionSignal)
    .map {
      case (marker, mousePosition) =>
        GameMarkerInfo.FixedGameMarker(marker, mousePosition): GameMarkerInfo
    }

  EventStream.merge(fixedMarkers, onTargetMarkers)
    .map(info => List(UpdateMarker(0L, serverTime, info)))
    .map(InGameWSProtocol.GameActionWrapper)
    .foreach(socketOutWriter.onNext(_))
}
