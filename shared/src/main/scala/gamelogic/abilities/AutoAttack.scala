package gamelogic.abilities

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class AutoAttack(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id,
    damage: Double,
    cooldown: Long,
    resourceType: Resource,
    range: Distance
) extends WithTargetAbility {
  def abilityId: AbilityId = Ability.autoAttackId

  def castingTime: Long = 0L

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(EntityTakesDamage(genActionId(), time, targetId, damage, casterId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRange(gameState, time) orElse
      isInSight(gameState, time)

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, resourceType)
}
