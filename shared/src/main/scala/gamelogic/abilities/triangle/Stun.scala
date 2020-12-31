package gamelogic.abilities.triangle

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Entity
import gamelogic.abilities.WithTargetAbility
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.Resource.Energy
import gamelogic.gamestate.GameState
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameAction

/**
  * Impeach the target from doing anything for the next 20 seconds.
  * If the target takes damage, the effect is cancelled.
  * If the ability is used on another target, the effect is cancelled.
  */
final case class Stun(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
  extends WithTargetAbility {
    def abilityId: AbilityId = Ability.triangleStun
    def cooldown: Long = Stun.cooldown
    def castingTime: Long = 0L
    def cost: ResourceAmount = Stun.cost
    def range: Distance = Stun.range

    def createActions(gameState: GameState)(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = ???

    def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

    def canBeCast(gameState: GameState, time: Long): Boolean = 
      canBeCastEnemyOnly(gameState) && isInRangeAndInSight(gameState, time)
}

object Stun {

  @inline def cooldown: Long = 10000L
  @inline def cost: ResourceAmount = ResourceAmount(50.0, Energy)
  @inline def range: Distance = 300.0
}