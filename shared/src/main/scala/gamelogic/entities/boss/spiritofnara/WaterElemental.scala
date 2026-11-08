package gamelogic.entities.boss.spiritofnara

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Constants
import gamelogic.entities.{
  Entity,
  LivingEntity,
  MovingBody,
  Resource,
  WithAbilities,
  WithTarget,
  WithThreat
}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Shape}

/** Summoned by the Spirit of Nara when it falls under one of the
  * [[gamelogic.abilities.boss.spiritofnara.SummonWaterElementals.lifeThresholds]] of its life.
  *
  * A Water Elemental doesn't move. It hurls Waterbolts at random players until it is killed, or
  * until it vanishes (see [[gamelogic.buffs.boss.spiritofnara.Ephemeral]]). It can be stunned, and
  * its Waterbolts can be cut.
  */
final case class WaterElemental(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    rotation: Angle,
    life: Double,
    damageThreats: Map[Id, ThreatAmount],
    targetId: Entity.Id,
    relevantUsedAbilities: Map[AbilityId, Ability]
) extends MovingBody
    with LivingEntity
    with WithThreat
    with WithTarget
    with WithAbilities {

  def direction: Angle = Angle.zero
  def speed: Double    = 0.0
  def moving: Boolean  = false

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): WaterElemental = copy(time = time, pos = position, rotation = rotation)

  def maxLife: Double = WaterElemental.maxLife

  override def canBeStunned: Boolean = true

  protected def patchLifeTotal(newLife: Double): WaterElemental = copy(life = newLife)

  def healingThreats: Map[Id, ThreatAmount] = Map()

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): WaterElemental =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): WaterElemental = this

  def changeTarget(newTargetId: Id): WaterElemental = copy(targetId = newTargetId)

  def abilities: Set[AbilityId] = Set(Ability.spiritOfNaraWaterbolt)

  def useAbility(ability: Ability): WaterElemental =
    copy(relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability))

  def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  def maxResourceAmount: Double = 0.0

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): WaterElemental =
    this

  def shape: ConvexPolygon = WaterElemental.shape

  def teamId: TeamId = Entity.teams.mobTeam
}

object WaterElemental {

  inline def maxLife: Double = 1200.0

  /** A triangle, like all adds. */
  val shape: ConvexPolygon = Shape.regularPolygon(3, 2 * Constants.playerRadius)

}
