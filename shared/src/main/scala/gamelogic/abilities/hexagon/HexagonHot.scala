package gamelogic.abilities.hexagon

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.EntityIdGenerator

final case class HexagonHot(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends Ability {
  val abilityId: AbilityId = Ability.hexagonHexagonHotId
  val cooldown: Long       = 4000L
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(15, Mana)

  def createActions(gameState: GameState, entityIdGenerator: EntityIdGenerator): List[GameAction] = ???

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability = copy(time = newTime, useId = newId)
}
