package gamelogic.abilities.square

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.ThreatToEntityChange
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/**
  * Increases the damage threat level this caster has towards the target.
  */
final case class Taunt(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  val abilityId: AbilityId = Ability.squareTauntId
  val cooldown: Long       = Ability.gcd
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(10, Rage)

  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    ThreatToEntityChange(
      0L,
      time,
      targetId,
      casterId,
      Taunt.damageThreatAmount,
      isDamageThreat = true
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Taunt =
    copy(useId = newId, time = newTime)

  def range: Distance = WithTargetAbility.meleeRange

  def canBeCast(gameState: GameState, time: UseId): Boolean =
    canBeCastEnemyOnly(gameState) && isInRangeAndInSight(gameState, time)
}

object Taunt {

  @inline final def damageThreatAmount = 400.0

}
