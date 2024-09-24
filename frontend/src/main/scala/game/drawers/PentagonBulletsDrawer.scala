package game.drawers

import indigo.*
import gamelogic.gamestate.GameState

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import gamelogic.physics.Complex
import game.gameutils.*
import gamelogic.entities.Entity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.classes.pentagon.PentagonZone
import assets.Asset
import models.bff.outofgame.PlayerClasses

object PentagonBulletsDrawer {

  private val playerColorsCache: LocalCache[Entity.Id, Map[Entity.Id, PlayerClass], RGBA] =
    LocalCache((playerId, players) =>
      players
        .get(playerId)
        .fold(RGBA.Black)(player =>
          val color = player.rgb
          RGBA.fromColorInts(color._1, color._2, color._3)
        )
    )

  def drawBullets(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.pentagonBullets.values.toJSArray.map { bullet =>
      val position   = bullet.currentPosition(now)
      val ownerColor = playerColorsCache.retrieve(bullet.ownerId, gameState.players)

      Shape.Circle(
        center = gameToLocal(position),
        radius = bullet.shape.radius.toInt,
        fill = Fill.Color(ownerColor)
      )
    }

  def drawPentaZones(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[PentagonZone].values.toJSArray.map { zone =>
    val position   = zone.pos
    val rotation   = zone.rotation
    val ownerColor = zone.colour.toIndigo

    drawPentaZone(position, rotation, gameToLocal, zone.shape.radius, ownerColor)
  }

  private def drawPentaZone(
      position: Complex,
      rotation: Double,
      gameToLocal: Complex => Point,
      radius: Double,
      color: RGBA
  ) =
    Shape.Polygon(
      vertices = Batch(
        gamelogic.physics.shape.Shape
          .regularPolygon(5, radius)
          .vertices
          .toJSArray
          .map(_ * Complex.rotation(rotation) + position)
          .reverse
          .map(gameToLocal)
      ),
      fill = Fill.Color(color.withAlpha(0.3)),
      stroke = Stroke(2, color)
    )

  def drawAll(
      gameState: GameState,
      maybePositionToPlaceZone: Option[Complex],
      myId: Entity.Id,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    drawBullets(gameState, now, gameToLocal) ++ drawPentaZones(
      gameState,
      now,
      gameToLocal
    ) ++ maybePositionToPlaceZone.toJSArray.map { position =>
      drawPentaZone(
        position,
        0,
        gameToLocal,
        PentagonZone.shape.radius,
        playerColorsCache.retrieve(myId, gameState.players)
      )
    }

  private val pentagonZoneAsset = Asset.playerClassAssetMap(PlayerClasses.Pentagon)

}
