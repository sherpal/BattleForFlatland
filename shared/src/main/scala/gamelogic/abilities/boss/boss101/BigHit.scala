package gamelogic.abilities.boss.boss101

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.gameactions.EntityTakesDamage

final case class BigHit(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {

  def abilityId: Ability.AbilityId = Ability.boss101BigHitId

  def cooldown: Long = 120000L

  def castingTime: Long = 1000L

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(
      gameState: GameState
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction] =
    List(
      EntityTakesDamage(
        0L,
        time,
        targetId,
        BigHit.damageAmount,
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability = copy(time = newTime, useId = newId)

  def range: Distance = Boss101.meleeRange

  def canBeCast(gameState: GameState, time: Long): Boolean =
    canBeCastEnemyOnly(gameState) && isInRange(gameState, time)
}

object BigHit {

  final def damageAmount: Double = 150.0

}
