package gamelogic.abilities

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState

trait WithTargetAbility extends Ability {

  def range: Distance
  def targetId: Entity.Id

  final def isInRange(gameState: GameState, time: Long): Boolean =
    (for {
      caster <- gameState.withPositionEntityById(casterId)
      target <- gameState.withPositionEntityById(targetId)
      casterPosition = caster.currentPosition(time)
      targetPosition = target.currentPosition(time)
      distance       = (casterPosition - targetPosition).modulus
    } yield distance <= range).getOrElse(false)

  final def isInSight(gameState: GameState, time: Long): Boolean =
    gameState.areTheyInSight(casterId, targetId, time).getOrElse(false)

  final def isInRangeAndInSight(gameState: GameState, time: Long): Boolean =
    isInRange(gameState, time) && isInSight(gameState, time)

  final def canBeCastFriendlyOnly(gameState: GameState): Boolean =
    gameState.areTheyFromSameTeam(casterId, targetId).getOrElse(false)

  final def canBeCastEnemyOnly(gameState: GameState): Boolean = !canBeCastFriendlyOnly(gameState)

  /**
    * Returns whether this target can be stunned.
    */
  final def targetCanBeStunned(gameState: GameState): Boolean =
    gameState.livingEntityById(entityId = targetId).map(_.canBeStunned).getOrElse(false)

}

object WithTargetAbility {

  /** Represents a distance in the Complex plane (usually computed using (z1 - z2).modulus) */
  type Distance = Double

  // todo: change that
  def meleeRange: Distance = Constants.playerRadius * 2 + Boss101.shape.radius

  def healRange: Distance = 600.0
}
