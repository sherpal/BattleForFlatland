package gamelogic.abilities.triangle

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.Silenced
import gamelogic.entities.Resource.Energy
import gamelogic.entities.{Entity, Resource, WithAbilities}
import gamelogic.gamestate.gameactions.{EntityCastingInterrupted, PutSimpleBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

case class Cut(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  override def range: Distance = Cut.range

  override def abilityId: AbilityId = Ability.triangleCut

  override def cooldown: Long = Cut.cooldown

  override def castingTime: Long = 0L

  override def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Energy)

  override def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      EntityCastingInterrupted(genActionId(), time, targetId),
      PutSimpleBuff(genActionId(), time, genBuffId(), targetId, casterId, time, Buff.silence)
    )

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  override def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState)
      .orElse(isInRangeAndInSight(gameState, time))
      .orElse(canCutTarget(gameState))

  private def canCutTarget(gameState: GameState): Option[String] =
    Option
      .unless(gameState.entityIsCasting(targetId))("Target does not cast anything")
      .orElse(gameState.castingEntityInfo.get(targetId) match {
        case None =>
          Some("Target does not cast anything") // should not happen as it's checked before
        case Some(castingInfo) =>
          Option.unless(castingInfo.ability.canBeCut)("Ability of target can't be cut.")
      })
}

object Cut {
  val range: Distance = WithTargetAbility.healRange

  val cooldown: Long = 5000L
}
