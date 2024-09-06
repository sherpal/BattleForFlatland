package gamelogic.abilities.triangle

import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.Ability
import gamelogic.entities.Entity
import gamelogic.entities.Resource
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.entities.Resource.Energy
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.gameactions.EntityResourceChanges

/** Gives back some energy to the caster, while dealing small damages to the target.
  *
  * This is what is called a "filler" in the field: an ability that the player can always do when
  * they have nothing else to do. Otherwise a [[gamelogic.entities.classes.Triangle]] spends most of
  * their time waiting for energy, which is no fun.
  */
final case class EnergyKick(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {

  def abilityId: Ability.AbilityId = Ability.triangleEnergyKick

  def cooldown: Long = EnergyKick.cooldown

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Energy)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = Vector(
    EntityTakesDamage(genActionId(), time, targetId, EnergyKick.damage, casterId),
    EntityResourceChanges(
      genActionId(),
      time,
      casterId,
      EnergyKick.energyGain,
      Energy
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)

  def range: WithTargetAbility.Distance = WithTargetAbility.meleeRange

}

object EnergyKick {
  @inline val cooldown: Long     = 700L
  @inline val energyGain: Double = 5.0
  @inline val damage: Double     = 15.0 // todo: fix this value
}
