package game.drawers

import gamelogic.physics.Complex

import gamelogic.gamestate.GameState

import indigo.Point

import indigo.SceneNode
import gamelogic.gameextras.GameMarkerInfo
import gamelogic.gameextras.GameMarkerInfo.FixedGameMarker
import gamelogic.gameextras.GameMarkerInfo.GameMarkerOnEntity
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import assets.Asset
import indigo.*

object GameMarkersDrawer extends Drawer {

  def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = {
    val (onEntity, onGround) = gameState.markersInfo.values.toJSArray.partitionMap {
      case f: FixedGameMarker    => Right(f)
      case e: GameMarkerOnEntity => Left(e)
    }

    drawMarkersOnEntity(onEntity, gameState, now, gameToLocal) ++ drawMarkersOnGround(
      onGround,
      gameToLocal
    )
  }

  private def drawMarkersOnEntity(
      markers: js.Array[GameMarkerInfo.GameMarkerOnEntity],
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = for {
    info   <- markers
    entity <- gameState.movingBodyEntityById(info.entityId)
  } yield {
    val asset = Asset.markerAssetMap(info.marker)
    asset.indigoGraphic(
      gameToLocal(entity.currentPosition(gameState.time)),
      None,
      Radians.zero,
      targetSize
    )
  }

  private def drawMarkersOnGround(
      markers: js.Array[GameMarkerInfo.FixedGameMarker],
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = markers.map { info =>
    val asset = Asset.markerAssetMap(info.marker)
    asset.indigoGraphic(
      gameToLocal(info.gamePosition),
      None,
      Radians.zero,
      targetSize
    )
  }

  private val targetSize = Size(16)

}
