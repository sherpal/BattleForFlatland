package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss102.{PutDamageZones, PutLivingDamageZoneOnTarget, SpawnHound}
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.utils.IdGeneratorContainer
import models.syntax.Pointed
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.buffs.Buff
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.docs.BossMetadata
import models.bff.outofgame.PlayerClasses

final case class Boss102(
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

  def name: String = Boss102.name

  def shape: Circle = Boss101.shape

  def abilities: Set[AbilityId] = Boss102.abilities

  def useAbility(ability: Ability): Boss102 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): Boss102 =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  protected def patchLifeTotal(newLife: Double): Boss102 =
    copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss102 =
    copy(damageThreats =
      damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss102 =
    copy(healingThreats =
      healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeTarget(newTargetId: Id): Boss102 = copy(targetId = newTargetId)

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Boss102 = this

  def abilityNames: Map[AbilityId, String] = Map(
    Ability.boss102PutDamageZones -> "Damage zones",
    Ability.boss102SpawnBossHound -> "Spawn Hound",
    Ability.putLivingDamageZoneId -> "Living damage zone",
    Ability.autoAttackId          -> "Auto attack"
  )

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        Boss102.autoAttackDamage,
        Boss102.autoAttackTickRate,
        NoResource,
        Boss102.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbility(_, time).isEmpty)

}

object Boss102 extends BossFactory[Boss102] with BossMetadata {

  import Complex.i

  override def maybeAIComposition: Option[List[PlayerClasses]] = Some(
    List(
      PlayerClasses.Square,
      PlayerClasses.Pentagon,
      PlayerClasses.Pentagon,
      PlayerClasses.Hexagon,
      PlayerClasses.Hexagon
    )
  )

  final val shape: Circle = new Circle(30.0)

  final val maxLife: Double = 40000

  val intendedFor = 5 // players

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  final val autoAttackDamage: Double = 10.0
  final val autoAttackTickRate: Long = 3000L

  inline def fullSpeed: Double = 300.0

  def initialBoss(entityId: Entity.Id, time: Long): Boss102 =
    Pointed[Boss102].unit
      .copy(
        id = entityId,
        time = time,
        speed = fullSpeed,
        relevantUsedAbilities = Map(
          Ability.boss102PutDamageZones -> Pointed[PutDamageZones].unit.copy(
            time = time - PutDamageZones.cooldown + PutDamageZones.timeToFirstAbility
          ),
          Ability.boss102SpawnBossHound -> Pointed[SpawnHound].unit.copy(
            time = time - SpawnHound.cooldown + SpawnHound.timeToFirstAbility
          ),
          Ability.putLivingDamageZoneId -> Pointed[PutLivingDamageZoneOnTarget].unit.copy(
            time =
              time - PutLivingDamageZoneOnTarget.cooldown + PutLivingDamageZoneOnTarget.timeToFirstAbility
          )
        ),
        maxLife = maxLife,
        life = maxLife
      )

  def initialBossActions(
      entityId: Entity.Id,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    healAndDamageAwareActions(entityId, time)

  val size = 350.0
  def gameBoundariesActions(
      time: Long
  )(using IdGeneratorContainer): Vector[CreateObstacle] =
    Vector[(Complex, (Complex, Complex))](
      (size, (-i * size, i * size)),
      (-size, (-i * size, i * size)),
      (-i * size, (-size, size)),
      (i * size, (-size, size))
    ).map { case (position, (z1, z2)) =>
      CreateObstacle(
        genActionId(),
        time,
        genEntityId(),
        position,
        Obstacle.segmentObstacleVertices(z1, z2, 10)
      )
    }

  def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameBoundariesActions(time)

  def playersStartingPosition: Complex = -100 * i

  val name: String = "Boss 102"

  final val abilities: Set[Ability.AbilityId] =
    Set(
      Ability.boss102PutDamageZones,
      Ability.boss102SpawnBossHound,
      Ability.autoAttackId,
      Ability.putLivingDamageZoneId
    )

  def whenBossDiesActions(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameState.entities.values.collect {
      case hound: BossHound =>
        RemoveEntity(genActionId(), time, hound.id)
      case zone: DamageZone =>
        RemoveEntity(genActionId(), time, zone.id)
    }.toVector ++ gameState.allBuffs.collect {
      case buff: Buff
          if Vector(
            Buff.boss102DamageZoneBuff,
            Buff.boss102LivingDamageZone
          ) contains buff.resourceIdentifier =>
        RemoveBuff(genActionId(), time, buff.bearerId, buff.buffId)
    }
}
