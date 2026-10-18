package gamelogic.entities.classes

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.triangle.{Cut, DirectHit, EnergyKick, Stun, UpgradeDirectHit}
import gamelogic.buffs.Buff
import gamelogic.buffs.abilities.classes.{
  Silenced,
  TriangleStunDebuff,
  UpgradeDirectHit as UpgradeDirectHitBuff
}
import gamelogic.buffs.resourcebuffs.EnergyFiller
import gamelogic.docs.{formatNumber, formatSeconds, AbilityDoc}
import gamelogic.entities.Resource.{Energy, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.PutSimpleBuff
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Polygon, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses

/** The [[Triangle]] is the melee dps class in BFF. It attacks close its enemies.
  *
  * The [[Triangle]] has an energy bar that is replenish very often (10 seconds to replenish it
  * entirely), but most of the attacks uses a lot amount of them.
  */
final case class Triangle(
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
) extends PlayerClass(PlayerClasses.Triangle) {

  def shape: Polygon = Triangle.shape

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Triangle = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  def abilities: Set[AbilityId] = Triangle.abilities

  def useAbility(ability: Ability): Triangle = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount = resourceAmount - ability.cost
  )

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): Triangle =
    copy(resourceAmount = newResourceAmount)

  protected def patchLifeTotal(newLife: Double): Triangle = copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.playerTeam
}

object Triangle extends PlayerClassBuilder {

  val shape: ConvexPolygon = Shape.regularPolygon(3, Constants.playerRadius)

  def initialResourceAmount: ResourceAmount = ResourceAmount(100, Energy)

  val initialMaxLife: Double = 100

  def description: String =
    "The melee damage dealer. The Triangle fights in contact with its enemies, and has tools to " ++
      "control them: it can stun adds and interrupt their spells. Its energy refills by " ++
      s"${formatNumber(EnergyFiller.energyRefillPerSecond)} every second, but its big hits are " ++
      "costly."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.triangleDirectHit,
      "Direct Hit",
      s"Hits an enemy in melee range for ${formatNumber(DirectHit.directHitDamage)} damage.",
      Ability.gcd,
      0L,
      DirectHit.cost
    ),
    AbilityDoc(
      Ability.triangleEnergyKick,
      "Energy Kick",
      s"Kicks an enemy in melee range for ${formatNumber(EnergyKick.damage)} damage, and gives " ++
        s"you back ${formatNumber(EnergyKick.energyGain)} energy. Use it while waiting for energy.",
      EnergyKick.cooldown,
      0L
    ),
    AbilityDoc(
      Ability.triangleUpgradeDirectHit,
      "Upgrade Direct Hit",
      "Increases the damage of your Direct Hit by " ++
        s"${formatNumber((UpgradeDirectHitBuff.damageIncrease - 1) * 100)}% for " ++
        s"${formatSeconds(UpgradeDirectHitBuff.duration)}. Can be stacked.",
      0L,
      0L,
      UpgradeDirectHit.cost
    ),
    AbilityDoc(
      Ability.triangleStun,
      "Stun",
      s"Stuns an enemy (within ${formatNumber(Stun.range)} units) for up to " ++
        s"${formatSeconds(TriangleStunDebuff.duration)}, interrupting what it was casting. The " ++
        "stun breaks as soon as the target takes damage, or if you stun another target. Bosses " ++
        "can't be stunned.",
      Stun.cooldown,
      0L,
      Stun.cost
    ),
    AbilityDoc(
      Ability.triangleCut,
      "Cut",
      "Interrupts the spell an enemy is casting (if that spell can be interrupted), and " ++
        s"silences it for ${formatSeconds(Silenced.duration)}.",
      Cut.cooldown,
      0L
    )
  )

  val abilities: Set[Ability.AbilityId] = Set(
    Ability.triangleEnergyKick,
    Ability.triangleDirectHit,
    Ability.triangleUpgradeDirectHit,
    Ability.triangleStun,
    Ability.triangleCut
  )

  def startingActions(
      time: Long,
      entityId: Entity.Id
  )(using IdGeneratorContainer): Vector[GameAction] = Vector(
    PutSimpleBuff(
      GameAction.Id.zero,
      time,
      genBuffId(),
      entityId,
      entityId,
      time,
      Buff.energyFiller
    )
  )
}
