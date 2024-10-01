package game.drawers

import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import indigo.*
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object ObstacleDrawer extends Drawer {

  def drawAll(gameState: GameState, now: Long, gameToLocal: Complex => Point): js.Array[SceneNode] =
    gameState.obstacles.values.toJSArray.map { obstacle =>
      Shape
        .Polygon(
          vertices = Batch(
            obstacle.shape.vertices.toJSArray
              .map(_ + obstacle.pos)
              .reverse
              .map(gameToLocal)
          ),
          fill = Fill.Color(RGBA.White),
          stroke = Stroke(1, RGBA.fromColorInts(200, 200, 200))
        )
        .withDepth(Depth.far - 2)
    }
}
