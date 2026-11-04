package gamelogic.abilities.square

import gamelogic.abilities.Ability.{AbilityId, UseId}
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.buffs.abilities.classes.BraceShield
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{EntityTakesDamage, ThreatToEntityChange}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer

/** Spends a stack of the caster's [[gamelogic.buffs.abilities.classes.BraceShield]] to hit an
  * enemy in melee range, dealing more damage than a [[HammerHit]] and generating more threat.
  *
  * The remaining stacks of the shield get their time reset, like when bracing.
  */
final case class ShieldSlam(
    useId: Ability.UseId,
    time: Long,
    casterId: Entity.Id,
    targetId: Entity.Id
) extends WithTargetAbility {
  def range: Distance = WithTargetAbility.meleeRange

  val abilityId: AbilityId = Ability.squareShieldSlamId
  val cooldown: Long       = ShieldSlam.cooldown
  val castingTime: Long    = 0L

  def cost: Resource.ResourceAmount = ShieldSlam.cost

  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction] = {
    val remainingStacks =
      BraceShield.stacksOn(gameState, casterId).flatMap(_.minus(1))

    Vector(
      EntityTakesDamage(genActionId(), time, targetId, ShieldSlam.damage, casterId),
      ThreatToEntityChange(
        genActionId(),
        time,
        targetId,
        casterId,
        ShieldSlam.extraThreat,
        isDamageThreat = true
      )
    ) ++ BraceShield.setStacks(gameState, casterId, time, remainingStacks)
  }

  def copyWithNewTimeAndId(newTime: Long, newId: UseId): Ability =
    copy(time = newTime, useId = newId)

  def canBeCast(gameState: GameState, time: Long): Option[String] =
    canBeCastEnemyOnly(gameState)
      .orElse(isInRangeAndInSight(gameState, time))
      .orElse(
        Option.when(BraceShield.stacksOn(gameState, casterId).isEmpty)(
          "You need a stack of your shield"
        )
      )

}

object ShieldSlam {

  inline def damage: Double = 100.0

  /** Threat generated on top of the one that comes from the damage dealt. */
  inline def extraThreat: ThreatAmount = ThreatAmount.fromValue(100.0)

  inline def cooldown: Long = 3000L

  inline def cost: ResourceAmount = ResourceAmount(30, Rage)

}
