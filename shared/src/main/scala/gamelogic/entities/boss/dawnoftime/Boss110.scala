package gamelogic.entities.boss.dawnoftime

import gamelogic.entities.boss.BossEntity
import gamelogic.entities.boss.BossFactory

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss110.SpawnBigGuies
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
import gamelogic.entities.Resource
import gamelogic.abilities.boss.boss110.ExplodeBombs
import gamelogic.abilities.boss.boss110.PlaceBombPods
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.abilities.boss.boss110.SpawnSmallGuies

final case class Boss110(
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

  def abilityNames: Map[gamelogic.abilities.Ability.AbilityId, String] = Map(
    Ability.autoAttackId -> "Auto attack",
    Ability.boss110SpawnBigGuies -> SpawnBigGuies.name,
    Ability.boss110PlaceBombPods -> PlaceBombPods.name,
    Ability.boss110ExplodeBombs -> ExplodeBombs.name,
    Ability.boss110SpawnSmallGuies -> SpawnSmallGuies.name
  )

  def name: String = Boss110.name

  def shape: gamelogic.physics.shape.Circle = Boss110.shape

  // Members declared in gamelogic.entities.Entity
  def teamId: gamelogic.entities.Entity.TeamId = Entity.teams.mobTeam

  // Members declared in gamelogic.entities.LivingEntity
  protected def patchLifeTotal(newLife: Double): Boss110 = copy(life = newLife)

  // Members declared in gamelogic.entities.MovingBody
  def move(
      time: Long,
      position: gamelogic.physics.Complex,
      direction: gamelogic.entities.WithPosition.Angle,
      rotation: gamelogic.entities.WithPosition.Angle,
      speed: Double,
      moving: Boolean
  ): Boss110 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  // Members declared in gamelogic.entities.WithAbilities
  def abilities: Set[gamelogic.abilities.Ability.AbilityId] = Boss110.abilities

  def maxResourceAmount: Double = 0.0

  protected def patchResourceAmount(
      newResourceAmount: gamelogic.entities.Resource.ResourceAmount
  ): Boss110 = this

  def resourceAmount: ResourceAmount = ResourceAmount(0.0, Resource.NoResource)

  def useAbility(ability: gamelogic.abilities.Ability): Boss110 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  // Members declared in gamelogic.entities.WithTarget
  def changeTarget(newTargetId: gamelogic.entities.Entity.Id): Boss110 = copy(targetId = newTargetId)

  // Members declared in gamelogic.entities.WithThreat
  def changeDamageThreats(
      threatId: gamelogic.entities.Entity.Id,
      delta: gamelogic.entities.WithThreat.ThreatAmount
  ): Boss110 = copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))
  def changeHealingThreats(
      threatId: gamelogic.entities.Entity.Id,
      delta: gamelogic.entities.WithThreat.ThreatAmount
  ): Boss110 = copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        0L,
        time,
        id,
        targetId,
        Boss110.autoAttackDamage,
        Boss110.autoAttackTickRate,
        NoResource,
        Boss110.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbility(_, time).isEmpty)

}

object Boss110 extends BossFactory[Boss110] {

  import Complex.i

  final val shape: Circle = new Circle(30.0)

  final val maxLife: Double = 100_000

  val intendedFor = 10 // players

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  final val autoAttackDamage: Double = 10.0
  final val autoAttackTickRate: Long = 1000L

  @inline final def fullSpeed: Double = 300.0

  def initialBoss(entityId: gamelogic.entities.Entity.Id, time: Long): gamelogic.entities.boss.dawnoftime.Boss110 =
    Pointed[Boss110].unit.copy(
      id      = entityId,
      time    = time,
      speed   = fullSpeed,
      life    = maxLife,
      maxLife = maxLife,
      relevantUsedAbilities = Map(
        SpawnBigGuies.abilityId -> Pointed[SpawnBigGuies].unit.copy(
          time = SpawnBigGuies.setBeginningOfGameAbilityUse(time)
        ),
        Ability.boss110PlaceBombPods -> Pointed[PlaceBombPods].unit.copy(
          time = time - PlaceBombPods.cooldown + PlaceBombPods.timeToFirstAbility
        ),
        Ability.boss110SpawnSmallGuies -> Pointed[SpawnSmallGuies].unit.copy(
          time = time - SpawnSmallGuies.cooldown + SpawnSmallGuies.timeToFirstAbility
        )
      )
    )

  def initialBossActions(
      entityId: gamelogic.entities.Entity.Id,
      time: Long,
      idGeneratorContainer: gamelogic.utils.IdGeneratorContainer
  ): List[gamelogic.gamestate.GameAction] =
    healAndDamageAwareActions(entityId, time, idGeneratorContainer)

  def name: String = "Boss 110"

  def playersStartingPosition: gamelogic.physics.Complex = 0

  val halfWidth  = 600
  val halfHeight = 300

  def gameBoundariesActions(time: Long, idGeneratorContainer: IdGeneratorContainer): List[CreateObstacle] =
    List[(Complex, (Complex, Complex))](
      (halfWidth, (-i * halfHeight, i * halfHeight)),
      (-halfWidth, (-i * halfHeight, i * halfHeight)),
      (-i * halfHeight, (-halfWidth, halfWidth)),
      (i * halfHeight, (-halfWidth, halfWidth))
    ).map {
      case (position, (z1, z2)) =>
        CreateObstacle(
          idGeneratorContainer.gameActionIdGenerator(),
          time,
          idGeneratorContainer.entityIdGenerator(),
          position,
          Obstacle.segmentObstacleVertices(z1, z2, 10)
        )
    }

  def stagingBossActions(
      time: Long,
      idGeneratorContainer: gamelogic.utils.IdGeneratorContainer
  ): List[gamelogic.gamestate.GameAction] =
    gameBoundariesActions(time, idGeneratorContainer)

  def whenBossDiesActions(
      gameState: gamelogic.gamestate.GameState,
      time: Long,
      idGeneratorContainer: gamelogic.utils.IdGeneratorContainer
  ): List[gamelogic.gamestate.GameAction] =
    gameState.entities.values
      .collect {
        case bigGuy: BigGuy => bigGuy
        case bomb: BombPod  => bomb
      }
      .map(entity => RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), time, entity.id))
      .toList

  final val abilities: Set[Ability.AbilityId] =
    Set(
      Ability.autoAttackId,
      Ability.boss110SpawnBigGuies,
      Ability.boss110ExplodeBombs,
      Ability.boss110PlaceBombPods,
      Ability.boss110SpawnSmallGuies
    )

}
