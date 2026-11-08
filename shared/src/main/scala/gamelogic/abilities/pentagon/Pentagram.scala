package gamelogic.abilities.pentagon

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.abilities.classes.Facets
import gamelogic.entities.classes.pentagon.SteadyAim
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.EntityTakesDamage
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import services.i18n.I18n

/** Spends all the [[gamelogic.buffs.abilities.classes.Facets]] of the caster, which must all be
  * lit, to hit an enemy from afar.
  *
  * It is instant, so that it is what the Pentagon does while it has to move.
  */
final case class Pentagram(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  def range: Distance = WithTargetAbility.healRange

  def abilityId: AbilityId = Ability.pentagonPentagramId

  def cooldown: Long = Ability.gcd

  def castingTime: Long = 0L

  def cost: Resource.ResourceAmount = Pentagram.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] =
    EntityTakesDamage(
      genActionId(),
      time,
      targetId,
      Pentagram.damage * SteadyAim.damageMultiplier(gameState, casterId, time),
      casterId,
      abilityId
    ) +: Facets.setStacks(gameState, casterId, time, None)

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long)(using i18n: I18n): Option[String] =
    canBeCastEnemyOnly(gameState)
      .orElse(isInRangeAndInSight(gameState, time))
      .orElse(
        Option.unless(Facets.stacksOn(gameState, casterId).contains(Facets.maxStacks))(
          i18n.needAllFacets
        )
      )

}

object Pentagram {

  inline def damage: Double = 70.0

  inline def cost: Resource.ResourceAmount = Resource.ResourceAmount(0.0, Resource.Mana)

}
