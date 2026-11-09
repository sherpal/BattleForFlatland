package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.square.{Brace, Cleave, Enrage, Guard, HammerHit, ShieldSlam, Taunt}
import gamelogic.abilities.WithTargetAbility
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{BraceShield, Guarding}
import gamelogic.buffs.resourcebuffs.{RageFiller, RageTrickle}
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds, AbilityDoc}
import gamelogic.entities.Resource.{Rage, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, LivingEntity}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses

import scala.collection.immutable.ListSet

/** The [[gamelogic.entities.classes.Square]] is the tank class available to players.
  */
final case class Square(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    direction: Angle,
    moving: Boolean,
    rotation: Angle,
    life: Double,
    colour: Int,
    relevantUsedAbilities: Map[AbilityId, Ability],
    maxLife: Double,
    speed: Double,
    resourceAmount: ResourceAmount,
    maxResourceAmount: Double,
    name: String
) extends PlayerClass(PlayerClasses.Square) {
  protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  def abilities: Set[AbilityId] = Square.abilities

  def useAbility(ability: Ability): Square = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount = resourceAmount - ability.cost
  )

  def shape: Polygon = Square.shape

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Square =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  def teamId: Entity.TeamId = Entity.teams.playerTeam

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Square =
    copy(resourceAmount = newResourceAmount)
}

object Square extends PlayerClassBuilder {

  final val shape = Shape.regularPolygon(4, Constants.playerRadius)

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Rage)

  // A ListSet, so that the order of the abilities (and thus their keys) is the one of their group
  // in `Ability`.
  final val abilities: Set[AbilityId] =
    ListSet.from(Ability.square.toList)

  final val initialMaxLife: Double = 200

  def description: String =
    s"The tank. With its ${formatNumber(initialMaxLife)} life points and a shield it has to " ++
      "keep building up, the Square is the one keeping the boss' attention. It gains " ++
      s"${formatNumber(RageFiller.ragePerDamage)} rage for each point of damage it takes (before " ++
      s"its shield reduces it), and ${formatNumber(RageTrickle.rageRefillPerSecond)} rage every " ++
      "second whatever happens. It spends its rage on its abilities."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.square.hammerHitId,
      "Hammer Hit",
      s"Strikes an enemy in melee range for ${formatNumber(HammerHit.damage)} damage.",
      Ability.gcd,
      0L,
      HammerHit.cost
    ),
    AbilityDoc(
      Ability.square.tauntId,
      "Taunt",
      s"Generates ${formatNumber(Taunt.damageThreatAmount.value)} threat towards an enemy " ++
        s"(within ${formatNumber(WithTargetAbility.healRange)} units), so that it attacks you.",
      Ability.gcd,
      0L,
      Taunt.cost
    ),
    AbilityDoc(
      Ability.square.cleaveId,
      "Cleave",
      s"Strikes all enemies in a cone in front of you for ${formatNumber(Cleave.damage)} damage.",
      Cleave.cooldown,
      0L,
      Cleave.cost
    ),
    AbilityDoc(
      Ability.square.braceId,
      "Brace",
      s"Adds a stack to your shield, up to ${BraceShield.maxStacks} stacks. Each stack reduces " ++
        s"all damage you take by ${formatNumber((1 - BraceShield.damageMultiplier) * 100)}%. " ++
        s"Your shield loses a stack after ${formatSeconds(BraceShield.duration)}, unless you " ++
        "Brace again, which also resets that time when the shield is full.",
      Brace.cooldown,
      0L,
      Brace.cost
    ),
    AbilityDoc(
      Ability.square.shieldSlamId,
      "Shield Slam",
      s"Spends a stack of your shield to strike an enemy in melee range for " ++
        s"${formatNumber(ShieldSlam.damage)} damage, and generates an additional " ++
        s"${formatNumber(ShieldSlam.extraThreat.value)} threat. The stacks you keep have their " ++
        "time reset.",
      ShieldSlam.cooldown,
      0L,
      ShieldSlam.cost
    ),
    AbilityDoc(
      Ability.square.guardId,
      "Guard",
      s"Protects an ally (within ${formatNumber(WithTargetAbility.healRange)} units) for " ++
        s"${formatSeconds(Guarding.duration)}: ${formatNumber(Guarding.redirectedShare * 100)}% of " ++
        "the damage it takes is taken by you instead, as if it was hitting you (your shield " ++
        "reduces it, and it gives you rage). You can only guard one ally at a time.",
      Guard.cooldown,
      0L,
      Guard.cost
    ),
    AbilityDoc(
      Ability.square.enrageId,
      "Enrage",
      s"Hurts yourself for ${formatNumber(Enrage.damageOnTick)} damage every " ++
        s"${formatPeriod(Enrage.tickRate)} for ${formatSeconds(Enrage.duration)}. Since taking " ++
        "damage gives you rage, use it to build up rage when the boss is not hitting you.",
      Enrage.cooldown,
      0L
    )
  )

  def startingActions(
      time: Long,
      entityId: Entity.Id
  )(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutSimpleBuff(
        GameAction.Id.zero,
        time,
        genBuffId(),
        entityId,
        entityId,
        time,
        Buff.rageFiller
      ),
      PutSimpleBuff(
        GameAction.Id.zero,
        time,
        genBuffId(),
        entityId,
        entityId,
        time,
        Buff.rageTrickle
      )
    )
}
