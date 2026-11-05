package bffclient.hud

import gamelogic.entities.Entity
import models.bff.ingame.Controls

/** What the frames of the HUD need from the client: who we are, our target, and our controls (for
  * the keys of the action bar). The [[bffclient.game.GameClient]] in a game, a fake one in the
  * tutorial (which shows the HUD's frames with a made-up game state).
  */
trait HudClient {
  def controls: Controls
  def myId: Option[Entity.Id]
  def currentTarget: Option[Entity.Id]

  /** Whether we face our target (shown by a lock on its frame). */
  def lockInToTarget: Boolean

  /** Targets this entity (e.g. its frame was clicked). */
  def selectTarget(id: Entity.Id): Unit
}
