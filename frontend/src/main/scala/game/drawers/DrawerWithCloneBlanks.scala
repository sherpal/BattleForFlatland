package game.drawers

import indigo.*

import scala.scalajs.js
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex

trait DrawerWithCloneBlanks extends Drawer {

  def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point): Layer.Content

}

object DrawerWithCloneBlanks {
  val empty: DrawerWithCloneBlanks = new DrawerWithCloneBlanks {
    export Drawer.empty.*

    override def cloneLayer(gameState: GameState, now: Long, gameToLocal: Complex => Point) =
      Layer.empty
  }
}
