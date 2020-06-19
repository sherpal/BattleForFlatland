package gamelogic.entities.boss.boss102

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities._
import gamelogic.entities.classes.Constants
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}

final case class BossHound(
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

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): BossHound =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def maxLife: Double = BossHound.houndMaxLife

  protected def patchLifeTotal(newLife: Double): BossHound = copy(life = newLife)

  def shape: Polygon = BossHound.shape

  def healingThreats: Map[Id, ThreatAmount] = Map() // don't care about healing threat

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): BossHound = copy(
    damageThreats = damageThreats + (threatId -> damageThreats.get(threatId).fold(delta)(_ + delta))
  )

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): BossHound = this

  def changeTarget(newTargetId: Id): BossHound = copy(targetId = newTargetId)

  def abilities: Set[AbilityId] = Set(Ability.autoAttackId)

  def useAbility(ability: Ability): BossHound =
    copy(
      relevantUsedAbilities =
        relevantUsedAbilities + (ability.abilityId -> ability)
    )

  def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  def maxResourceAmount: Double = 0.0

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): BossHound = this

  def maybeAutoAttack(time: Long): Option[AutoAttack] =
    Some(
      AutoAttack(
        0L,
        time,
        id,
        targetId,
        BossHound.damageOnTick,
        BossHound.tickRate,
        NoResource,
        BossHound.range * 2
      )
    ).filter(canUseAbility(_, time))

}

object BossHound {

  @inline final def houndMaxLife = 800.0
  val fullSpeed: Double          = Constants.playerSpeed * 6 / 5
  val damageOnTick               = 5.0
  val tickRate                   = 1000L
  val range: Double              = Constants.playerRadius * 2

  final val shape = Shape.regularPolygon(3, Constants.playerRadius)

  @inline final def name: String = "Boss Hound 102"

}
