package gamelogic.abilities.hexagon

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.UpdateConstantHot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}

final case class HexagonHot(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends Ability {
  val abilityId: AbilityId = Ability.hexagonHexagonHotId
  val cooldown: Long       = 4000L
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(15, Mana)

  def createActions(
      gameState: GameState
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction] = List(
    UpdateConstantHot(
      0L,
      time,
      targetId,
      buffIdGenerator(),
      HexagonHot.duration,
      HexagonHot.tickRate,
      HexagonHot.healOnTick,
      casterId,
      time
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)
}

object HexagonHot {

  @inline final def healOnTick = 5.0
  @inline final def duration   = 15000L
  @inline final def tickRate   = 3000L

}
