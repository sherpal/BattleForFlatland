package gamelogic.abilities.boss.boss101

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class SmallHit(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id,
    damage: Double
) extends WithTargetAbility {
  def range: Distance = Boss101.meleeRange

  def abilityId: AbilityId = Ability.boss101SmallHitId

  def cooldown: Long = SmallHit.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = SmallHit.cost

  def createActions(
      gameState: GameState
  )(using IdGeneratorContainer): Vector[GameAction] = Vector(
    EntityTakesDamage(
      genActionId(),
      time,
      targetId,
      damage,
      casterId
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): SmallHit =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)
}

object SmallHit {
  final val name: String         = "SmallHit"
  final def damageAmount: Double = 15.0
  final def cooldown: Long       = 2500L
  final def cost: ResourceAmount = ResourceAmount(0.0, NoResource)
}
