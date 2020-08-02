package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.dawnoftime.Boss102.healAndDamageAwareActions
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.gamestate.GameAction
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.utils.IdGeneratorContainer

final case class Boss103(
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

  def name: String = Boss103.name

  def shape: Circle = Boss101.shape

  def abilityNames: Map[AbilityId, String] = ???

  def changeTarget(newTargetId: Id): Boss103 = copy(targetId = newTargetId)

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  protected def patchLifeTotal(newLife: Double): Boss103 = copy(life = newLife)

  def abilities: Set[AbilityId] = ???

  def useAbility(ability: Ability): Boss103 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): Boss103 = this

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Boss103 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def teamId: Entity.TeamId = Entity.teams.mobTeam
}

object Boss103 extends BossFactory[Boss103] {
  def initialBoss(entityId: Id, time: Id): Boss103 = ???

  def initialBossActions(entityId: Id, time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    healAndDamageAwareActions(entityId, time, idGeneratorContainer)

  def stagingBossActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = ???

  def playersStartingPosition: Complex = 0

  def name: String = "Boss 103"
}
