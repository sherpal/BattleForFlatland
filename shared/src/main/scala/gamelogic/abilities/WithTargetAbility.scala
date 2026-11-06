package gamelogic.abilities

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.entities.boss.Boss101
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState
import services.i18n.I18n

trait WithTargetAbility extends Ability {

  def range: Distance
  def targetId: Entity.Id

  final def isInRange(gameState: GameState, time: Long)(using i18n: I18n): Option[String] =
    Option.unless((for {
      caster <- gameState.withPositionEntityById(casterId)
      target <- gameState.withPositionEntityById(targetId)
      casterPosition = caster.currentPosition(time)
      targetPosition = target.currentPosition(time)
      distance       = (casterPosition - targetPosition).modulus
    } yield distance <= range).getOrElse(false))(i18n.notInRange)

  final def isInSight(gameState: GameState, time: Long)(using i18n: I18n): Option[String] =
    gameState.areTheyInSight(casterId, targetId, time) match {
      case Some(inSight) => Option.unless(inSight)(i18n.notInSight)
      case None => Some(i18n.entitiesDoNotExist(casterId, targetId))
    }

  final def isInRangeAndInSight(gameState: GameState, time: Long)(using I18n): Option[String] =
    isInRange(gameState, time) orElse isInSight(gameState, time)

  final def canBeCastFriendlyOnly(gameState: GameState)(using i18n: I18n): Option[String] =
    gameState.areTheyFromSameTeam(casterId, targetId) match {
      case Some(sameTeam) => Option.unless(sameTeam)(i18n.notAnAlly)
      case None => Some(i18n.entitiesDoNotExist(casterId, targetId))
    }

  final def canBeCastEnemyOnly(gameState: GameState)(using i18n: I18n): Option[String] =
    canBeCastFriendlyOnly(gameState) match {
      case None => Some(i18n.notAnEnemy)
      case _ => None
    }

  /** Returns whether this target can be stunned.
    */
  final def targetCanBeStunned(gameState: GameState): Boolean =
    gameState.livingEntityById(entityId = targetId).map(_.canBeStunned).getOrElse(false)

}

object WithTargetAbility {

  /** Represents a distance in the Complex plane (usually computed using (z1 - z2).modulus) */
  type Distance = Double

  // todo: change that
  inline def meleeRange: Distance = Constants.playerRadius * 2 + Boss101.shape.radius

  inline def healRange: Distance = 600.0

  inline def healRangeSquared: Double = healRange * healRange
}
