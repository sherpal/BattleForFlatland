package gamelogic.entities.boss.dawnoftime

import gamelogic.entities.boss.BossEntity
import gamelogic.entities.WithTarget
import gamelogic.physics.Complex
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.entities.Entity.TeamId
import gamelogic.entities.Entity.Id
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.WithAbilities
import gamelogic.entities.WithThreat
import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.shape.Circle
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.MovingBody
import gamelogic.entities.LivingEntity
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.Entity
import gamelogic.entities.boss.BossFactory
import gamelogic.gamestate.GameAction
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameState
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.physics.Complex.i
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.utils.IdsProducer
import models.syntax.Pointed
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.docs.BossMetadata
import models.bff.outofgame.PlayerClasses

final case class Boss104(
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
    healingThreats: Map[Entity.Id, ThreatAmount],
    damageThreats: Map[Entity.Id, ThreatAmount]
) extends BossEntity {

  override protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  override def shape: Circle = Boss101.shape

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(damageThreats =
      damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta))
    )

  override protected def patchResourceAmount(newResourceAmount: ResourceAmount): WithAbilities =
    this

  override def abilities: Set[AbilityId] = Set(
    Ability.autoAttackId
  )

  override def abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId -> "Auto Attack"
  )

  override def teamId: TeamId = Entity.teams.mobTeam

  override def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  override def useAbility(ability: Ability): WithAbilities = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  override def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): MovingBody = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  override def changeTarget(newTargetId: Id): WithTarget = copy(targetId = newTargetId)

  override def changeHealingThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(healingThreats =
      healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta))
    )

  override def maxResourceAmount: Double = 0.0

  override def name: String = Boss104.name

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        Boss104.autoAttackDamage,
        Boss104.autoAttackTickRate,
        NoResource,
        Boss104.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbility(_, time).isEmpty)

}

object Boss104 extends BossFactory[Boss104] with BossMetadata {

  override def intendedFor: Int = 5

  override def maybeAIComposition: Option[List[PlayerClasses]] = None

  inline def shape: Circle = Boss101.shape

  inline def meleeRange: Distance = shape.radius + 20.0
  inline def rangeRange: Distance = 2000.0 // basically infinite distance

  inline def autoAttackDamage: Double = 5.0
  inline def autoAttackTickRate: Long = 1000L

  inline def maxLife: Double = 30000
  override def initialBoss(entityId: Id, time: Long): Boss104 = Pointed[Boss104].unit.copy(
    id = entityId,
    time = time,
    maxLife = maxLife,
    life = maxLife,
    speed = Boss102.fullSpeed
  )

  override def initialBossActions(entityId: Id, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = healAndDamageAwareActions(entityId, time)

  override def whenBossDiesActions(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  val size = 500.0

  def gameBoundariesActions(
      time: Long
  )(using IdGeneratorContainer): Vector[CreateObstacle] = Obstacle.squareGameArea(time, size)

  def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameBoundariesActions(time)

  override def playersStartingPosition: Complex = 0

  val name: String = "Boss 104"

}
