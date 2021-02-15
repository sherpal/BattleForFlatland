package gamelogic.entities.boss.boss110

import gamelogic.physics.shape.Circle
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

final case class SmallGuy(
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
  ): SmallGuy =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def maxLife: Double = SmallGuy.maxLife

  protected def patchLifeTotal(newLife: Double): SmallGuy = copy(life = newLife)

  def shape: Circle = SmallGuy.shape

  def healingThreats: Map[Id, ThreatAmount] = Map() // don't care about healing threat

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): SmallGuy = copy(
    damageThreats = damageThreats + (threatId -> damageThreats.get(threatId).fold(delta)(_ + delta))
  )

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): SmallGuy = this

  def changeTarget(newTargetId: Id): SmallGuy = copy(targetId = newTargetId)

  def abilities: Set[AbilityId] = Set(Ability.autoAttackId)

  def useAbility(ability: Ability): SmallGuy =
    copy(
      relevantUsedAbilities =
        relevantUsedAbilities + (ability.abilityId -> ability)
    )

  def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  def maxResourceAmount: Double = 0.0

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): SmallGuy = this

  def maybeAutoAttack(time: Long): Option[AutoAttack] =
    Some(
      AutoAttack(
        0L,
        time,
        id,
        targetId,
        SmallGuy.damageOnTick,
        SmallGuy.tickRate,
        NoResource,
        SmallGuy.range * 2
      )
    ).filter(canUseAbilityBoolean(_, time))

  def canBeStunned: Boolean = true

}

object SmallGuy {

  def maxLife: Double   = 700.0
  val fullSpeed: Double = Constants.playerSpeed
  val damageOnTick      = 2.0
  val tickRate          = 2000L
  val range: Double     = Constants.playerRadius * 2

  val shape = new Circle(Constants.playerRadius)

}
