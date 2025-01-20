package game.drawers

import gamelogic.physics.Complex
import gamelogic.entities.WithPosition.Angle
import indigo.*
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import gamelogic.gamestate.GameState
import gamelogic.entities.classes.hexagon.HexagonZone
import game.gameutils.toIndigo
import gamelogic.entities.Entity
import game.gameutils.LocalCache
import gamelogic.entities.classes.PlayerClass

object HexagonZoneDrawer {

  def drawAll(
      gameState: GameState,
      maybePositionToPlaceZone: Option[Complex],
      myId: Entity.Id,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    drawHexaZones(gameState, now, gameToLocal) ++ maybePositionToPlaceZone.toJSArray.map {
      position =>
        drawHexaZone(
          position,
          0,
          gameToLocal,
          HexagonZone.shape.radius,
          playerColorsCache.retrieve(myId, gameState.players)
        )
    }

  def drawHexaZones(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[HexagonZone].values.toJSArray.map { zone =>
    val position   = zone.pos
    val rotation   = zone.rotation
    val ownerColor = zone.colour.toIndigo

    drawHexaZone(position, rotation, gameToLocal, zone.shape.radius, ownerColor)
  }

  private def drawHexaZone(
      position: Complex,
      rotation: Angle,
      gameToLocal: Complex => Point,
      radius: Double,
      color: RGBA
  ) = Shape.Polygon(
    vertices = Batch(
      gamelogic.physics.shape.Shape
        .regularPolygon(6, radius)
        .vertices
        .toJSArray
        .map(_ * Complex.rotation(rotation) + position)
        .reverse
        .map(gameToLocal)
    ),
    fill = Fill.Color(color.withAlpha(0.3)),
    stroke = Stroke(2, color)
  )

  private val playerColorsCache: LocalCache[Entity.Id, Map[Entity.Id, PlayerClass], RGBA] =
    LocalCache((playerId, players) =>
      players
        .get(playerId)
        .fold(RGBA.Black)(player =>
          val color = player.rgb
          RGBA.fromColorInts(color._1, color._2, color._3)
        )
    )

}
