package gamelogic.abilities.square

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class HammerHit(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  def range: Distance = WithTargetAbility.meleeRange

  val abilityId: AbilityId = Ability.squareHammerHit
  val cooldown: Long       = Ability.gcd
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(20, Rage)

  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(EntityTakesDamage(0L, time, targetId, HammerHit.damage, casterId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean =
    canBeCastEnemyOnly(gameState) && isInRangeAndInSight(gameState, time)

}

object HammerHit {

  @inline def damage: Double = 10.0

}
