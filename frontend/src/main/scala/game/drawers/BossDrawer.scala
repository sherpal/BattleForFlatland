package game.drawers

import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import indigo.*
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object BossDrawer extends Drawer {

  override def drawAll(
      gameState: GameState,
      now: Long,
      gameToLocal: Complex => Point
  ): js.Array[SceneNode] = gameState.bosses.values.headOption.toJSArray.flatMap { boss =>
    val currentBossPosition = boss.currentPosition(gameState.time)
    val barPos = gameToLocal(currentBossPosition + Complex.i * (boss.shape.radius + 10))

    val localBossPos = gameToLocal(currentBossPosition)

    Shape
      .Circle(
        localBossPos,
        boss.shape.radius.toInt,
        Fill.Color(RGBA.White)
      )
      .withDepth(Depth(4)) +: Shape
      .Line(
        localBossPos,
        gameToLocal(currentBossPosition + boss.shape.radius * Complex.rotation(boss.rotation)),
        Stroke(2, RGBA.Black)
      )
      .withDepth(Depth(4)) +: minilifebar(boss, barPos).presentWithChildrenWithoutRectangle
  }

}
