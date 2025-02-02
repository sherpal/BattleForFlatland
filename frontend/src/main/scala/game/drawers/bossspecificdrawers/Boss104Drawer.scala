package game.drawers.bossspecificdrawers

import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import indigo.*
import scala.scalajs.js
import gamelogic.entities.boss.boss104.DebuffCircle
import scala.scalajs.js.JSConverters.*
import game.gameutils.toIndigo
import indigo.shared.scenegraph.Layer.Content
import gamelogic.buffs.boss.boss104.TwinDebuff

object Boss104Drawer extends game.drawers.DrawerWithCloneBlanks {

  override def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point): Content =
    Content.empty

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    drawDebuffCircles(gameState, gameToLocal) ++ drawDebuffColoursOnPlayers(gameState, gameToLocal)

  private def drawDebuffCircles(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[DebuffCircle].values.toJSArray.map { circle =>
    Shape
      .Circle(
        gameToLocal(circle.pos),
        circle.shape.radius.toInt,
        Fill.Color(circle.colour.toIndigo.withAlpha(0.5))
      )
      .withDepth(Depth.far)
  }

  private def drawDebuffColoursOnPlayers(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState
    .allTBuffs[TwinDebuff]
    .flatMap(buff => gameState.players.get(buff.bearerId).map(buff -> _))
    .toJSArray
    .map { (buff, player) =>
      val pos    = player.currentPosition(gameState.time)
      val colour = buff.colour.toIndigo.withAlpha(0.5)
      val radius = (player.shape.radius * 1.5).toInt
      Shape
        .Circle(gameToLocal(pos), radius, Fill.Color(colour))
        .withDepth(Depth.far)
    }

}
