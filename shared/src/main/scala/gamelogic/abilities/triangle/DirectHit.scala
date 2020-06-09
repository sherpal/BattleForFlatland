package gamelogic.abilities.triangle

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Resource.{Energy, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class DirectHit(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id, damage: Double)
    extends WithTargetAbility {
  def abilityId: AbilityId = Ability.triangleDirectHit

  def cooldown: Long = Ability.gcd

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = DirectHit.cost

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    EntityTakesDamage(idGeneratorContainer.gameActionIdGenerator(), time, targetId, damage, casterId)
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean =
    canBeCastEnemyOnly(gameState) && isInRangeAndInSight(gameState, time)

  def range: Distance = WithTargetAbility.meleeRange
}

object DirectHit {

  @inline final def directHitDamage: Double = 200.0

  @inline final def cost: ResourceAmount = ResourceAmount(30.0, Energy)

}
