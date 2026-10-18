package gamelogic.abilities.square

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.ThreatToEntityChange
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Increases the damage threat level this caster has towards the target.
  */
final case class Taunt(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  val abilityId: AbilityId = Ability.squareTauntId
  val cooldown: Long       = Ability.gcd
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = Taunt.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = Vector(
    ThreatToEntityChange(
      genActionId(),
      time,
      targetId,
      casterId,
      Taunt.damageThreatAmount,
      isDamageThreat = true
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Taunt =
    copy(useId = newId, time = newTime)

  def range: Distance = WithTargetAbility.healRange

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)
}

object Taunt {

  inline def damageThreatAmount: ThreatAmount = ThreatAmount.fromValue(400.0)

  inline def cost: ResourceAmount = ResourceAmount(10, Rage)

}
