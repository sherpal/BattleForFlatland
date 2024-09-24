package game.drawers.bossspecificdrawers

import indigo.*
import gamelogic.gamestate.GameState

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import gamelogic.physics.Complex
import game.gameutils.toIndigo
import assets.Asset
import gamelogic.buffs.boss.boss102.LivingDamageZone

object Boss102Drawer extends game.drawers.Drawer {

  def drawAll(gameState: GameState, now: Long, gameToLocal: Complex => Point): js.Array[SceneNode] =
    drawLivingDamageZones(gameState, now, gameToLocal)

  private def drawLivingDamageZones(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ) =
    gameState.players.toJSArray
      .flatMap((id, player) =>
        gameState.allBuffsOfEntity(id).collectFirst { case ldz: LivingDamageZone =>
          player -> ldz
        }
      )
      .map[SceneNode] { (player, debuff) =>
        livingDamageZoneAsset.indigoGraphic(
          gameToLocal(player.currentPosition(now)),
          None,
          Radians.zero,
          Size((2 * LivingDamageZone.range).toInt)
        )
      }

  private val livingDamageZoneAsset = Asset.ingame.gui.boss.dawnOfTime.boss102.livingDamageZone

}
