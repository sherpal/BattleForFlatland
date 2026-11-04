package gamelogic.abilities.square

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.classes.square.PutGuarding
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Protects an ally: for a while, a share of the damage that it takes is taken by the caster
  * instead (see [[gamelogic.buffs.abilities.classes.Guarding]]), and the ally has a
  * [[gamelogic.buffs.abilities.classes.Guarded]] buff to show it.
  *
  * The cooldown is longer than the duration of the guard, so that a caster protects one ally at a
  * time.
  */
final case class Guard(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  def range: Distance = WithTargetAbility.healRange

  val abilityId: AbilityId = Ability.squareGuardId
  val cooldown: Long       = Guard.cooldown
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = Guard.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(PutGuarding(genActionId(), time, genBuffId(), casterId, targetId, genBuffId()))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastFriendlyOnly(gameState)
      .orElse(Option.when(targetId == casterId)("You can't guard yourself"))
      .orElse(isInRangeAndInSight(gameState, time))

}

object Guard {

  inline def cooldown: Long = 10000L

  inline def cost: ResourceAmount = ResourceAmount(20, Rage)

}
