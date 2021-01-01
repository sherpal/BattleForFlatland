package gamelogic.abilities.boss.boss101

import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class BigHit(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {

  def abilityId: Ability.AbilityId = Ability.boss101BigHitId

  def cooldown: Long = 120000L

  def castingTime: Long = 1000L

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
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

  def range: Distance = Boss101.rangeRange

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)
}

object BigHit {

  final val name: String = "BigHit"

  final def damageAmount: Double = 150.0

  final val timeToFirstBigHit: Long = 15000L
  final val cooldown: Long          = 120000L

}
