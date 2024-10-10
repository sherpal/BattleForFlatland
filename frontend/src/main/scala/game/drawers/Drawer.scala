package game.drawers

import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import indigo.*
import scala.scalajs.js

trait Drawer {

  def drawAll(gameState: GameState, now: Long, gameToLocal: Complex => Point): js.Array[SceneNode]

}

object Drawer {
  val empty: Drawer = new Drawer {
    def drawAll(
        gameState: GameState,
        now: Long,
        gameToLocal: Complex => Point
    ): js.Array[SceneNode] = js.Array()
  }
}
