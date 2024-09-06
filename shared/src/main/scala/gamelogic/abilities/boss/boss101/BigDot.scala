package gamelogic.abilities.boss.boss101

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.PutConstantDot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class BigDot(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  def abilityId: AbilityId = Ability.boss101BigDotId

  def cooldown: Long = BigDot.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(0, NoResource)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutConstantDot(
        genActionId(),
        time,
        targetId,
        casterId,
        BigDot.damageOnTick,
        BigDot.duration,
        BigDot.tickRate,
        genBuffId(),
        Buff.boss101BigDotIdentifier
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def range: Distance = Boss101.rangeRange

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)
}

object BigDot {

  final val name: String = "BigDot"

  final def damageOnTick: Double = 20.0
  final def duration: Long       = 60000L
  final def tickRate: Long       = 3000L

  final val timeToFirstBigDot: Long = 10000L
  final val cooldown: Long          = 20000L

}
