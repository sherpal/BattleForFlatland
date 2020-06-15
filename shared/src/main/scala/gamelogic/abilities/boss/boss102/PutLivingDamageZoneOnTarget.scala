package gamelogic.abilities.boss.boss102

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.boss102.PutLivingDamageZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class PutLivingDamageZoneOnTarget(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  def abilityId: AbilityId = Ability.putLivingDamageZoneId

  def cooldown: Long = PutLivingDamageZoneOnTarget.cooldown

  def castingTime: Long = PutLivingDamageZoneOnTarget.castingTime

  def cost: Resource.ResourceAmount = PutLivingDamageZoneOnTarget.cost

  def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      PutLivingDamageZone(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        idGeneratorContainer.buffIdGenerator(),
        targetId,
        PutLivingDamageZoneOnTarget.damage,
        casterId
      )
    )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Boolean = true

  def range: Distance = Boss102.rangeRange
}

object PutLivingDamageZoneOnTarget {

  @inline final def cooldown: Long                = LivingDamageZone.duration / 2
  @inline final def castingTime: Long             = 0L
  @inline final def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.NoResource)
  @inline final def damage: Double                = 30.0
  @inline final def timeToFirstLivingDZ: Long     = 15000L

}
