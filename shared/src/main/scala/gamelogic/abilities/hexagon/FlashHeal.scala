package gamelogic.abilities.hexagon

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Entity
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.classes.{Constants, Hexagon}
import gamelogic.gamestate.gameactions.EntityGetsHealed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator, IdGeneratorContainer}

final case class FlashHeal(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  val abilityId: AbilityId = Ability.hexagonFlashHealId
  val cooldown: Long       = 0L
  val castingTime: Long    = 1000L

  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(EntityGetsHealed(0L, time, targetId, FlashHeal.healAmount, casterId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): FlashHeal = copy(time = newTime, useId = newId)

  val cost: ResourceAmount = ResourceAmount(10, Mana)

  def range: Distance = WithTargetAbility.healRange

  def canBeCast(gameState: GameState, time: UseId): Boolean =
    canBeCastFriendlyOnly(gameState) && isInRange(gameState, time)
}

object FlashHeal {

  final val healAmount: Double = 15

}
