package gamelogic.entities.boss.boss104

import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{
  Entity,
  LivingEntity,
  MovingBody,
  Resource,
  WithAbilities,
  WithTarget,
  WithThreat
}
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Shape}

final case class BigGuy(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    direction: Angle,
    rotation: Angle,
    speed: Double,
    moving: Boolean,
    life: Double,
    damageThreats: Map[Id, ThreatAmount],
    targetId: Entity.Id,
    relevantUsedAbilities: Map[AbilityId, Ability]
) extends MovingBody
    with LivingEntity
    with WithThreat
    with WithTarget
    with WithAbilities {
  override def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): BigGuy = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  override def maxLife: Double = BigGuy.maxLife

  override def canBeStunned: Boolean = true

  override protected def patchLifeTotal(newLife: ThreatAmount): BigGuy = copy(life = newLife)

  override def healingThreats: Map[Id, ThreatAmount] = Map() // don't care about healing threat

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): BigGuy = copy(
    damageThreats = damageThreats + (threatId -> damageThreats.get(threatId).fold(delta)(_ + delta))
  )

  override def changeHealingThreats(threatId: Id, delta: ThreatAmount): BigGuy = this

  override def changeTarget(newTargetId: Id): BigGuy = copy(targetId = newTargetId)

  override def abilities: Set[AbilityId] = Set(Ability.autoAttackId, Ability.boss104BigGuyKick)

  override def useAbility(ability: Ability): BigGuy = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  override def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  override def maxResourceAmount: ThreatAmount = 0.0

  override protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): BigGuy =
    this

  override def shape: ConvexPolygon = BigGuy.shape

  override def teamId: TeamId = Entity.teams.mobTeam

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        BigGuy.damageOnTick,
        BigGuy.tickRate,
        NoResource,
        BigGuy.range * 2
      )
    ).filter(canUseAbilityBoolean(_, time, gameState))
}

object BigGuy {
  inline def maxLife: Double = 800.0

  val shape: ConvexPolygon = Shape.regularPolygon(3, 2 * Constants.playerRadius)
  val fullSpeed: Double    = Constants.playerSpeed * 2 / 5
  val range: Double        = shape.radius + Constants.playerRadius
  val damageOnTick         = 3.0
  val tickRate             = 1200L
}
