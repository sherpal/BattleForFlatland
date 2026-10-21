package gamelogic.abilities.boss.boss105

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{EntityGetsHealed, EntityTakesDamage}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** The Shade drains its target, and gives what it takes to the boss. Can be interrupted (with the
  * Triangle's Cut, for example).
  */
final case class Siphon(useId: Ability.UseId, time: Long, casterId: Entity.Id, targetId: Entity.Id)
    extends WithTargetAbility {
  def abilityId: AbilityId = Ability.boss105ShadeSiphon

  def cooldown: Long = Siphon.cooldown

  def castingTime: Long = Siphon.castingTime

  def cost: Resource.ResourceAmount = Resource.noResourceAmount

  override def canBeCut: Boolean = true

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    EntityTakesDamage(genActionId(), time, targetId, Siphon.damage, casterId) +:
      gameState.bosses.values.toVector.map(boss =>
        EntityGetsHealed(genActionId(), time, boss.id, boss.maxLife * Siphon.healFraction, casterId)
      )

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Siphon =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState) orElse isInRangeAndInSight(gameState, time)

  def range: Distance = Siphon.range
}

object Siphon {

  val castingTime: Long = 2500L

  val cooldown: Long = 12000L

  val timeToFirstUse: Long = 5000L

  inline def damage: Double = 25.0

  /** Fraction of the maximum life of the boss that it heals. */
  inline def healFraction: Double = 0.025

  inline def range: Distance = 500.0
}
