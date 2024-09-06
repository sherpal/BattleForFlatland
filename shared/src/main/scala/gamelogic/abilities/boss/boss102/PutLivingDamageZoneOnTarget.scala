package gamelogic.abilities.boss.boss102

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.docs.{AbilityInfoFromMetadata, AbilityMetadata}
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss102.PutLivingDamageZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class PutLivingDamageZoneOnTarget(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility
    with AbilityInfoFromMetadata[PutLivingDamageZoneOnTarget.type] {
  def metadata = PutLivingDamageZoneOnTarget

  def cost: Resource.ResourceAmount = PutLivingDamageZoneOnTarget.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutLivingDamageZone(
        genActionId(),
        time,
        genBuffId(),
        targetId,
        PutLivingDamageZoneOnTarget.damage,
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): None.type = None

  def range: Distance = Boss102.rangeRange
}

object PutLivingDamageZoneOnTarget extends AbilityMetadata {

  def name = "Living Damage Zone"

  @inline final def cooldown: Long    = LivingDamageZone.duration / 2
  @inline final def castingTime: Long = 0L
  @inline final def cost: Resource.ResourceAmount =
    Resource.ResourceAmount(0.0, Resource.NoResource)
  @inline final def damage: Double           = 30.0
  @inline final def timeToFirstAbility: Long = 15000L

  def abilityId: Ability.AbilityId = Ability.putLivingDamageZoneId

}
