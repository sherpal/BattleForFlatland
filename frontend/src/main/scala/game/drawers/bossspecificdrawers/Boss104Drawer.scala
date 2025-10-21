package game.drawers.bossspecificdrawers

import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import indigo.*

import scala.scalajs.js
import gamelogic.entities.boss.boss104.{BigGuy, BigGuyDeathMark, DebuffCircle}

import scala.scalajs.js.JSConverters.*
import game.gameutils.toIndigo
import indigo.shared.scenegraph.Layer.Content
import gamelogic.buffs.boss.boss104.TwinDebuff
import utils.misc.RGBColour

object Boss104Drawer extends game.drawers.DrawerWithCloneBlanks {

  override def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point): Content =
    Content.empty

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    drawDebuffCircles(gameState, gameToLocal) ++ drawDebuffColoursOnPlayers(
      gameState,
      gameToLocal
    ) ++ drawBigGuys(gameState, gameToLocal) ++ drawBigGuyDeathMarkers(gameState, gameToLocal)

  private def drawBigGuyDeathMarkers(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] =
    gameState.allTEntities[BigGuyDeathMark].values.toJSArray.map { mark =>
      Shape
        .Circle(
          gameToLocal(mark.pos),
          mark.shape.radius.toInt,
          Fill.Color(RGBColour.black.withAlpha(0.5).toIndigo)
        )
        .withDepth(Depth.far)
    }

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

  private def drawBigGuys(
      gameState: GameState,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.allTEntities[BigGuy].values.toJSArray.flatMap { bigGuy =>
    val bigGuyPos = bigGuy.currentPosition(gameState.time)
    val rotation  = bigGuy.rotation
    val color     = RGBA.fromColorInts(50, 50, 50)
    val barPos    = gameToLocal(bigGuyPos + Complex.i * (bigGuy.shape.radius + 8))

    Shape.Polygon(
      Batch(
        bigGuy.shape.vertices.toJSArray.reverse
          .map(_ * Complex.rotation(rotation) + bigGuyPos)
          .map(gameToLocal)
      ),
      fill = Fill.Color(color.withAlpha(0.3)),
      stroke = Stroke(2, color)
    ) +: (game.drawers
      .minilifebar(bigGuy, barPos)
      .presentWithChildrenWithoutRectangle ++ game.drawers
      .minicastingbar(bigGuy, barPos - Point(0, 5), gameState)
      .presentWithChildrenWithoutRectangle)
  }

}
