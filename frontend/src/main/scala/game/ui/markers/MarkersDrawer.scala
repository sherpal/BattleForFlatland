package game.ui.markers

import typings.pixiJs.mod.Application
import assets.Asset
import typings.pixiJs.PIXI.{Container, LoaderResource}
import gamelogic.physics.Complex
import com.raquo.airstream.core.Observer
import game.Camera
import com.raquo.airstream.ownership.Owner
import com.raquo.airstream.eventbus.EventBus
import gamelogic.gamestate.GameState
import game.ui.reactivepixi.AttributeModifierBuilder._
import game.ui.reactivepixi.ReactivePixiElement._
import gamelogic.gameextras.GameMarker
import com.raquo.airstream.signal.Signal
import gamelogic.gameextras.GameMarkerInfo
import game.ui.reactivepixi.PixiModifier

final class MarkersDrawer(
    resources: PartialFunction[Asset, LoaderResource],
    camera: Camera,
    markersContainer: ReactiveContainer
)(implicit owner: Owner) {

  private val gameStateUpdatesBus: EventBus[(GameState, Long)] = new EventBus
  private val gameStates                                       = gameStateUpdatesBus.events

  /**
    * External world should feed [[GameState]]s here every time it should be re-rendered.
    */
  val gameStateWriter: Observer[(GameState, Long)] = gameStateUpdatesBus.writer

  private def makeMarkerElement(marker: GameMarker): PixiModifier[ReactiveContainer] = {
    val maybeMarkerPosition = gameStates
      .map {
        case (gs, time) =>
          for {
            markerInfo <- gs.maybeMarkerInfo(marker)
            position   <- markerInfo.maybePosition(gs, time)
          } yield camera.worldToLocal(position)
      }
      .startWith(None)
    pixiSprite(
      resources(Asset.markerAssetMap(marker)).texture,
      visible  <-- maybeMarkerPosition.map(_.isDefined),
      position <-- maybeMarkerPosition.map(_.getOrElse(0)),
      anchor := 0.5,
      dims := (20.0, 20.0)
    )
  }

  markersContainer.amend(GameMarker.allMarkers.map(makeMarkerElement): _*)

}
