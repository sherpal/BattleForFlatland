package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss101.{BigDot, BigHit, SmallHit}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.utils.IdGeneratorContainer
import models.syntax.Pointed

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

  def name: String = Boss101.name

  def shape: Circle = Boss101.shape

  def abilities: Set[AbilityId] = ???

  def useAbility(ability: Ability): Boss102 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Boss102 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  protected def patchLifeTotal(newLife: Double): Boss102 =
    copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss102 =
    copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss102 =
    copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  def changeTarget(newTargetId: Id): Boss102 = copy(targetId = newTargetId)

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Boss102 = this

  def abilityNames: Map[AbilityId, String] = ???

}

object Boss102 extends BossFactory[Boss102] {

  import Complex.i

  final val shape: Circle = new Circle(30.0)

  final val maxLife: Double = 20000

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  def initialBoss(entityId: Id, time: Id): Boss102 =
    Pointed[Boss102].unit
      .copy(
        id    = entityId,
        time  = time,
        speed = 300.0,
        relevantUsedAbilities = Map(
          ),
        maxLife = maxLife,
        life    = maxLife
      )

  def initialBossActions(entityId: Id, time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = ???

  val size = 350.0
  def gameBoundariesActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[CreateObstacle] =
    List[(Complex, (Complex, Complex))](
      (size, (-i * size, i * size)),
      (-size, (-i * size, i * size)),
      (-i * size, (-size, size)),
      (i * size, (-size, size))
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

  def stagingBossActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    gameBoundariesActions(time, idGeneratorContainer)

  def playersStartingPosition: Complex = -100 * i

  def name: String = "Boss 102"
}
