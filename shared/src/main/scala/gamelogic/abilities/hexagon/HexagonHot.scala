package gamelogic.abilities.hexagon

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.UpdateConstantHot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator, IdGeneratorContainer}

final case class HexagonHot(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  val abilityId: AbilityId = Ability.hexagonHexagonHotId
  val cooldown: Long       = 4000L
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(15, Mana)

  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction] = List(
    UpdateConstantHot(
      0L,
      time,
      targetId,
      idGeneratorContainer.buffIdGenerator(),
      HexagonHot.duration,
      HexagonHot.tickRate,
      HexagonHot.healOnTick,
      casterId,
      time
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: UseId): Boolean =
    canBeCastFriendlyOnly(gameState) && isInRange(gameState, time)

  def range: Distance = WithTargetAbility.healRange
}

object HexagonHot {

  @inline final def healOnTick = 5.0
  @inline final def duration   = 15000L
  @inline final def tickRate   = 3000L

}
