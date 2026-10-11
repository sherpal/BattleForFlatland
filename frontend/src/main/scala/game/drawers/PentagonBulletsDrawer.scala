package game.drawers

import game.gameutils.*
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.classes.pentagon.PentagonZone
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import indigo.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

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
      rotation: Angle,
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
        Angle.zero,
        gameToLocal,
        PentagonZone.shape.radius,
        playerColorsCache.retrieve(myId, gameState.players)
      )
    }

}
