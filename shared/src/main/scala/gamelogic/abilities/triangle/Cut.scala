package gamelogic.abilities.triangle

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.Energy
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{EntityCastingInterrupted, PutSimpleBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

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
    EntityCastingInterrupted(genActionId(), time, targetId) +:
      gameState.castingEntityInfo
        .get(targetId)
        .fold(
          Vector(
            PutSimpleBuff(genActionId(), time, genBuffId(), targetId, casterId, time, Buff.silence)
          )
        )(_.ability.cutPenalty(time, casterId))

  override def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  override def canBeCast(gameState: GameState, time: Long)(using I18n): Option[String] =
    canBeCastEnemyOnly(gameState)
      .orElse(isInRangeAndInSight(gameState, time))
      .orElse(canCutTarget(gameState))

  private def canCutTarget(gameState: GameState)(using i18n: I18n): Option[String] =
    Option
      .unless(gameState.entityIsCasting(targetId))(i18n.targetDoesNotCast)
      .orElse(gameState.castingEntityInfo.get(targetId) match {
        case None =>
          Some(i18n.targetDoesNotCast) // should not happen as it's checked before
        case Some(castingInfo) =>
          Option.unless(castingInfo.ability.canBeCut)(i18n.targetAbilityCannotBeCut)
      })
}

object Cut {
  val range: Distance = WithTargetAbility.healRange

  val cooldown: Long = 5000L
}
