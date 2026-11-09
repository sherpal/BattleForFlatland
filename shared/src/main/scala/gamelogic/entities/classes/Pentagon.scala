package gamelogic.entities.classes

import gamelogic.abilities.{Ability, WithTargetAbility}
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.pentagon.{
  CreatePentagonBullet,
  CreatePentagonZone,
  Fracture,
  PentaDispel,
  Pentagram
}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{Facets, Fractured}
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds, AbilityDoc}
import gamelogic.entities.classes.pentagon.{PentagonZone, SteadyAim}
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses

import scala.collection.immutable.ListSet

/** @param stillSince
  *   time since which the Pentagon has not moved, for its
  *   [[gamelogic.entities.classes.pentagon.SteadyAim]]
  */
final case class Pentagon(
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
    name: String,
    stillSince: Long
) extends PlayerClass(PlayerClasses.Pentagon) {
  def shape: Polygon = Pentagon.shape

  protected def patchLifeTotal(newLife: Double): Pentagon = copy(life = newLife)

  def abilities: Set[AbilityId] = Pentagon.abilities

  def useAbility(ability: Ability): Pentagon = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount = resourceAmount - ability.cost
  )

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Pentagon =
    copy(resourceAmount = newResourceAmount)

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Pentagon =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving,
      stillSince = if moving || this.moving || position != pos then time else stillSince
    )

  def teamId: TeamId = Entity.teams.playerTeam
}

object Pentagon extends PlayerClassBuilder {
  def startingActions(time: Long, entityId: Id)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      PutSimpleBuff(
        genActionId(),
        time,
        genBuffId(),
        entityId,
        entityId,
        time,
        Buff.manaFiller
      )
    )

  // A ListSet, so that the order of the abilities (and thus their keys) is the one of their group
  // in `Ability`.
  final val abilities: Set[Ability.AbilityId] =
    ListSet.from(Ability.pentagon.toList)

  final val shape: Polygon = Shape.regularPolygon(5, Constants.playerRadius)

  final val initialMaxLife: Double                = 100
  final val initialResourceAmount: ResourceAmount = ResourceAmount(300.0, Resource.Mana)

  def description: String =
    "The ranged damage dealer. The Pentagon shoots its enemies from afar, and is the only one " ++
      "able to dispel the nasty debuffs that bosses put on players. Its steady aim rewards " ++
      s"staying still: every ${formatPeriod(SteadyAim.stackEvery)} without moving, its abilities " ++
      s"deal ${formatNumber(SteadyAim.damageIncreasePerStack * 100)}% more damage, up to " ++
      s"${formatNumber(SteadyAim.maxStacks * SteadyAim.damageIncreasePerStack * 100)}%. Moving " ++
      "loses it all."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.pentagon.pentagonBulletId,
      "Pentagon Bullet",
      "Fires a bullet in the chosen direction. It travels up to " ++
        s"${formatNumber(PentagonBullet.defaultRange)} units and deals " ++
        s"${formatNumber(CreatePentagonBullet.damage)} damage to the first enemy it hits, " ++
        s"lighting one of your ${Facets.maxStacks} facets. A facet goes out after " ++
        s"${formatSeconds(Facets.duration)} without lighting any.",
      CreatePentagonBullet.cooldown,
      CreatePentagonBullet.castingTime,
      CreatePentagonBullet.cost
    ),
    AbilityDoc(
      Ability.pentagon.pentagonZoneId,
      "Pentagon Zone",
      "Creates a damaging zone at the chosen location for " ++
        s"${formatSeconds(PentagonZone.duration)}. Every ${formatPeriod(PentagonZone.tickRate)}, " ++
        s"enemies standing in it take ${formatNumber(PentagonZone.damageOnTick)} damage.",
      CreatePentagonZone.cooldown,
      CreatePentagonZone.castingTime,
      CreatePentagonZone.cost
    ),
    AbilityDoc(
      Ability.pentagon.fractureId,
      "Fracture",
      "Fires a bullet like Pentagon Bullet, which deals " ++
        s"${formatNumber(Fracture.damage)} damage and fractures the enemy it hits for " ++
        s"${formatSeconds(Fractured.duration)}: it takes " ++
        s"${formatNumber(Fractured.damageIncrease * 100)}% more damage from you. Fracturing it " ++
        "again adds what was left, up to " ++
        s"${formatSeconds(Fractured.maxCarriedOver)}. It does not light a facet.",
      Fracture.cooldown,
      Fracture.castingTime,
      Fracture.cost
    ),
    AbilityDoc(
      Ability.pentagon.pentagramId,
      "Pentagram",
      s"When your ${Facets.maxStacks} facets are lit, spends them all to deal " ++
        s"${formatNumber(Pentagram.damage)} damage to an enemy (within " ++
        s"${formatNumber(WithTargetAbility.healRange)} units). Instant: use it while you have to move.",
      Ability.gcd,
      0L,
      Pentagram.cost
    ),
    AbilityDoc(
      Ability.pentagon.dispelId,
      PentaDispel.name,
      "Removes all the effects that can be dispelled from an ally. Beware: this includes the " ++
        "beneficial ones!",
      PentaDispel.cooldown,
      0L,
      PentaDispel.cost
    )
  )
}
