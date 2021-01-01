package gamelogic.abilities

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState

trait WithTargetAbility extends Ability {

  def range: Distance
  def targetId: Entity.Id

  final def isInRange(gameState: GameState, time: Long): Option[String] =
    Option.unless((for {
      caster <- gameState.withPositionEntityById(casterId)
      target <- gameState.withPositionEntityById(targetId)
      casterPosition = caster.currentPosition(time)
      targetPosition = target.currentPosition(time)
      distance       = (casterPosition - targetPosition).modulus
    } yield distance <= range).getOrElse(false))("Not in range")

  final def isInSight(gameState: GameState, time: Long): Option[String] =
    gameState.areTheyInSight(casterId, targetId, time) match {
      case Some(inSight) => Option.unless(inSight)(s"Not in sight")
      case None          => Some(s"One of two entities $casterId and $targetId does not exist")
    }

  final def isInRangeAndInSight(gameState: GameState, time: Long): Option[String] =
    isInRange(gameState, time) orElse isInSight(gameState, time)

  final def canBeCastFriendlyOnly(gameState: GameState): Option[String] =
    gameState.areTheyFromSameTeam(casterId, targetId) match {
      case Some(sameTeam) => Option.unless(sameTeam)(s"Not an ally")
      case None           => Some(s"One or both of entities $casterId, $targetId does not exist")
    }

  final def canBeCastEnemyOnly(gameState: GameState): Option[String] =
    canBeCastFriendlyOnly(gameState) match {
      case None => Some(s"Not an ennemy")
      case _    => None
    }

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
