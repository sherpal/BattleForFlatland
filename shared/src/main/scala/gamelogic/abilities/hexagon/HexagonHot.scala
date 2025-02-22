package gamelogic.abilities.hexagon

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.UpdateConstantHot
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

final case class HexagonHot(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  val abilityId: AbilityId = Ability.hexagonHexagonHotId
  val cooldown: Long       = 4000L
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ResourceAmount(15, Mana)

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = Vector(
    UpdateConstantHot(
      genActionId(),
      time,
      targetId,
      genBuffId(),
      HexagonHot.duration,
      HexagonHot.tickRate,
      HexagonHot.healOnTick,
      casterId,
      time,
      Buff.hexagonHotIdentifier
    )
  )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastFriendlyOnly(gameState) orElse isInRangeAndInSight(gameState, time)

  def range: Distance = WithTargetAbility.healRange
}

object HexagonHot {

  inline def healOnTick = 15.0
  inline def duration   = 15000L
  inline def tickRate   = 3000L

}
