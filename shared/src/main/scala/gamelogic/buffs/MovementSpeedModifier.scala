package gamelogic.buffs

import gamelogic.entities.Entity
import gamelogic.entities.classes.{Constants, PlayerClass}
import gamelogic.gamestate.GameState

/** A [[Buff]] changing how fast its bearer moves (e.g., a slow).
  *
  * The speed of players is not decided by the action moving them (the client computes positions
  * from the speed it reads in the game state): [[gamelogic.gamestate.gameactions.MovingBodyMoves]]
  * always gives a player the speed [[MovementSpeedModifier.playerSpeed]] says. When such a buff
  * appears or disappears, a [[gamelogic.gamestate.gameactions.RefreshMovementSpeed]] updates the
  * bearer right away, so that a body moving in a straight line (which sends no new movement) slows
  * down too.
  *
  * Modifiers don't stack: only the strongest one applies.
  */
trait MovementSpeedModifier extends Buff {

  /** Factor applied to the normal speed of the bearer (0.5 halves it). */
  def speedMultiplier: Double

}

object MovementSpeedModifier {

  /** The factor applied to the speed of the entity, given its buffs. */
  def multiplier(gameState: GameState, entityId: Entity.Id): Double =
    gameState
      .allBuffsOfEntity(entityId)
      .collect { case modifier: MovementSpeedModifier => modifier.speedMultiplier }
      .minOption
      .getOrElse(1.0)

  /** The speed a player has, given its buffs. */
  def playerSpeed(gameState: GameState, player: PlayerClass): Double =
    Constants.playerSpeed * multiplier(gameState, player.id)

}
