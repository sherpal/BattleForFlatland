package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.pentagon.{CreatePentagonBullet, CreatePentagonZone, PentaDispel}
import gamelogic.buffs.Buff
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds, AbilityDoc}
import gamelogic.entities.classes.pentagon.PentagonZone
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
    name: String
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
      moving = moving
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

  final val abilities: Set[Ability.AbilityId] =
    Set(Ability.pentagonPentagonBullet, Ability.createPentagonZoneId, Ability.pentagonDispelId)

  final val shape: Polygon = Shape.regularPolygon(5, Constants.playerRadius)

  final val initialMaxLife: Double                = 100
  final val initialResourceAmount: ResourceAmount = ResourceAmount(300.0, Resource.Mana)

  def description: String =
    "The ranged damage dealer. The Pentagon shoots its enemies from afar, and is the only one " ++
      "able to dispel the nasty debuffs that bosses put on players."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.pentagonPentagonBullet,
      "Pentagon Bullet",
      "Fires a bullet in the chosen direction. It travels up to " ++
        s"${formatNumber(PentagonBullet.defaultRange)} units and deals " ++
        s"${formatNumber(CreatePentagonBullet.damage)} damage to the first enemy it hits.",
      CreatePentagonBullet.cooldown,
      CreatePentagonBullet.castingTime,
      CreatePentagonBullet.cost
    ),
    AbilityDoc(
      Ability.createPentagonZoneId,
      "Pentagon Zone",
      "Creates a damaging zone at the chosen location for " ++
        s"${formatSeconds(PentagonZone.duration)}. Every ${formatPeriod(PentagonZone.tickRate)}, " ++
        s"enemies standing in it take ${formatNumber(PentagonZone.damageOnTick)} damage.",
      CreatePentagonZone.cooldown,
      CreatePentagonZone.castingTime,
      CreatePentagonZone.cost
    ),
    AbilityDoc(
      Ability.pentagonDispelId,
      PentaDispel.name,
      "Removes all the effects that can be dispelled from an ally. Beware: this includes the " ++
        "beneficial ones!",
      PentaDispel.cooldown,
      0L,
      PentaDispel.cost
    )
  )
}
