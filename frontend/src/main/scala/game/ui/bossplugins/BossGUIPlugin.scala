package game.ui.bossplugins

import game.IndigoViewModel
import gamelogic.entities.Entity
import indigo.*

import scala.scalajs.js

/** Boss-specific tweaks to the *generic* UI components.
  *
  * Where `containerMapping` adds brand new components to the HUD, a plugin changes how existing
  * components look while a given boss is being fought. Each hook is consulted by the corresponding
  * component, and defaults to doing nothing, so a plugin only overrides the hooks it needs.
  *
  * Like drawers, plugins are pure functions of the game state: they read it and emit scene nodes,
  * they never decide anything.
  */
trait BossGUIPlugin {

  /** Extra scene nodes drawn on the [[game.ui.components.PlayerFrame]] of `playerId`.
    *
    * Nodes are drawn on top of the frame. To draw something *behind* the frame content (but still
    * above its background), use [[game.ui.components.PlayerFrame.overBackgroundDepth]].
    *
    * @param bounds
    *   the rectangle occupied by the frame on screen
    * @param alpha
    *   the alpha of the frame (lower when the player is out of healing range)
    */
  def playerFrameDecorations(playerId: Entity.Id, bounds: Rectangle, alpha: Double)(using
      IndigoViewModel
  ): js.Array[SceneNode] = js.Array()

}

object BossGUIPlugin {

  /** The plugin that changes nothing. */
  val empty: BossGUIPlugin = new BossGUIPlugin {}

  /** The plugin of the boss currently fought (or [[empty]] if there is no boss). */
  def current(using viewModel: IndigoViewModel): BossGUIPlugin =
    viewModel.gameState.bosses.values.headOption.fold(empty)(pluginMapping)

}
