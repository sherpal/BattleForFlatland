package gamelogic.abilities.hexagon

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.EntityGetsHealed
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}

final case class FlashHeal(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id) extends Ability {
  val abilityId: AbilityId = Ability.hexagonFlashHealId
  val cooldown: Long       = 0L
  val castingTime: Long    = 1000L

  def createActions(
      gameState: GameState
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction] =
    List(EntityGetsHealed(0L, time, targetId, FlashHeal.healAmount, casterId))

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): FlashHeal = copy(time = newTime, useId = newId)

  val cost: ResourceAmount = ResourceAmount(10, Mana)
}

object FlashHeal {

  final val healAmount: Double = 15

}
