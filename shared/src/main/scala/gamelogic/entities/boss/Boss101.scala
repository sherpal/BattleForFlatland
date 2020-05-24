package gamelogic.entities.boss
import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss101.BigDot
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.{Entity, WithAbilities, WithTarget, WithThreat}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import models.syntax.Pointed

/**
  * Very first boss to be coded. Probably not the most exiting one but the goal was to have a first proof of concept
  * and retrieve as much feedback as possible.
  *
  * The abilities of the boss are the following:
  * - "big" hit that directly attack the target (with a casting time so that the player can react). This attack will
  *   probably kill any player other than a tank (under cd)
  * - dot placed on someone different from the target (no casting time)
  * - spawn adds which will move towards and attack the player with the biggest healing threat (the heal if he is the
  *   only one). These adds will have melee attacks and move not too fast, leaving time to dps to kill them before
  *   they reach the heal.
  *
  * This boss is intended for 4 players (1 tank, 2 dps and 1 healer)
  */
final case class Boss101(
    id: Entity.Id,
    time: Long,
    targetId: Entity.Id,
    pos: Complex,
    rotation: Angle,
    direction: Angle,
    speed: Double,
    moving: Boolean,
    life: Double,
    maxLife: Double,
    relevantUsedAbilities: Map[AbilityId, Ability],
    healingThreats: Map[Id, ThreatAmount],
    damageThreats: Map[Id, ThreatAmount]
) extends BossEntity {

  def name: String = Boss101.name

  def shape: Circle = Boss101.shape

  def abilities: Set[AbilityId] = Set(Ability.boss101BigDotId, Ability.boss101BigHitId)

  def useAbility(ability: Ability): Boss101 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Boss101 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  protected def patchLifeTotal(newLife: Double): Boss101 =
    copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  def changeTarget(newTargetId: Id): WithTarget = copy(targetId = newTargetId)

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Boss101 = this
}

object Boss101 extends BossFactory {
  final val shape: Circle = new Circle(30.0)

  final val maxLife: Double = 1000

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  final val name: String = "Boss 101"

  def initialBoss(entityId: Entity.Id, time: Long): Boss101 =
    Pointed[Boss101].unit
      .copy(
        id    = entityId,
        time  = time,
        speed = 300.0,
        relevantUsedAbilities = Map(
          Ability.boss101BigDotId -> Pointed[BigDot].unit.copy(time = time - BigDot.timeToFirstBigDot)
        ),
        maxLife = maxLife,
        life    = maxLife
      )
}
