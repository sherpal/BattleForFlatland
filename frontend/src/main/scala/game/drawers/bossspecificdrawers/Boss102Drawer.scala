package game.drawers.bossspecificdrawers

import indigo.*
import gamelogic.gamestate.GameState

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import gamelogic.physics.Complex
import game.gameutils.toIndigo
import assets.Asset
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.boss.boss102.BossHound
import game.drawers.minilifebar
import game.drawers.LoopingAnimatedSprite
import gamelogic.entities.boss.boss102.DamageZone

object Boss102Drawer extends game.drawers.Drawer {

  def drawAll(gameState: GameState, now: Long, gameToLocal: Complex => Point): js.Array[SceneNode] =
    drawLivingDamageZones(gameState, now, gameToLocal) ++ drawBossHounds(
      gameState,
      gameToLocal
    ) ++ drawDamageZones(gameState, gameToLocal)

  private def drawBossHounds(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[BossHound].values.toJSArray.flatMap { hound =>
    val houndPos = hound.currentPosition(gameState.time)
    val rotation = hound.rotation
    val color    = RGBA.fromColorInts(50, 50, 50)
    val barPos   = gameToLocal(houndPos + Complex.i * (hound.shape.radius + 8))

    Shape.Polygon(
      Batch(
        hound.shape.vertices.toJSArray.reverse
          .map(_ * Complex.rotation(rotation) + houndPos)
          .map(gameToLocal)
      ),
      fill = Fill.Color(color.withAlpha(0.3)),
      stroke = Stroke(2, color)
    ) +: minilifebar(hound, barPos).presentWithChildrenWithoutRectangle
  }

  private def drawDamageZones(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[DamageZone].values.toJSArray.map { dz =>
    val localPos = gameToLocal(dz.pos)

    Shape
      .Circle(
        localPos,
        dz.shape.radius.toInt,
        Fill.Color(RGBA.Red.withAlpha(0.5))
      )
      .withDepth(Depth.far)
  }

  private def drawLivingDamageZones(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ) =
    gameState.players.toJSArray
      .flatMap((id, player) =>
        gameState.allBuffsOfEntity(id).collectFirst { case _: LivingDamageZone =>
          player
        }
      )
      .map[SceneNode] { player =>
        livingDamageZoneAnimatedSprite
          .graphic(gameState.time, Size((2 * LivingDamageZone.range * 192 / 120).toInt))
          .withPosition(gameToLocal(player.currentPosition(gameState.time)))
          .withRef(Point(192 / 2))
          .withDepth(Depth.far - 1)
      }

  private val livingDamageZoneAsset =
    Asset.ingame.gui.boss.dawnOfTime.boss102.livingDamageZoneAnimation

  private val livingDamageZoneAnimatedSprite =
    LoopingAnimatedSprite(livingDamageZoneAsset, 50L, 3, 5, Some(0.7))

}
