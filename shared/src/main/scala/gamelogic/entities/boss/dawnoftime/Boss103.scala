package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.dawnoftime.Boss102.{fullSpeed, healAndDamageAwareActions}
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.classes.Constants
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.syntax.Pointed

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

  def abilityNames: Map[AbilityId, String] = Map() // todo

  def changeTarget(newTargetId: Id): Boss103 = copy(targetId = newTargetId)

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  protected def patchLifeTotal(newLife: Double): Boss103 = copy(life = newLife)

  def abilities: Set[AbilityId] = Set() // todo

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

  final val maxLife: Double = 40000
  def initialBoss(entityId: Id, time: Id): Boss103 = Pointed[Boss103].unit.copy(
    id                    = entityId,
    time                  = time,
    maxLife               = maxLife,
    life                  = maxLife,
    speed                 = fullSpeed,
    relevantUsedAbilities = Map() // todo
  )

  def initialBossActions(entityId: Id, time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    healAndDamageAwareActions(entityId, time, idGeneratorContainer)

  final val roomRadius: Double           = 400.0
  final val pillarPositionRadius: Double = roomRadius * 6 / 10
  final val pillarRadius: Double         = Constants.playerRadius * 3
  final val sizeNumber: Int              = 6

  def stagingBossActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = {
    val pillarShape  = Shape.regularPolygon(3, pillarRadius)
    val wallLength   = 2 * roomRadius * math.sin(math.Pi / sizeNumber) // it's Hexagon so it equals roomRadius
    val wallVertices = Obstacle.segmentObstacleVertices(-wallLength / 2 * Complex.i, wallLength / 2 * Complex.i, 10)
//    def pillarAndWall(index: Int): (CreateObstacle, CreateObstacle) = {
//      val angle = 2 * math.Pi * index / sizeNumber
//
//      val pillarAction = CreateObstacle(
//        idGeneratorContainer.gameActionIdGenerator(),
//        time,
//        idGeneratorContainer.entityIdGenerator(),
//        pillarPositionRadius * Complex.rotation(angle + math.Pi / 6),
//        pillarShape.vertices.map(_ * Complex.rotation(angle + math.Pi / 6 + math.Pi))
//      )
//      val wallAction = CreateObstacle(
//        idGeneratorContainer.gameActionIdGenerator(),
//        time,
//        idGeneratorContainer.entityIdGenerator(),
//        roomRadius * math.cos(math.Pi / sizeNumber) * Complex.rotation(angle),
//        wallVertices.map(_ * Complex.rotation(angle))
//      )
//
//      (pillarAction, wallAction)
//    }
//
//    (0 to sizeNumber).toList.map(pillarAndWall).flatMap { case (_1, _2) => List(_1, _2) }
    def pillar(index: Int): CreateObstacle = {
      val angle = 2 * math.Pi * index / sizeNumber

      val pillarAction = CreateObstacle(
        idGeneratorContainer.gameActionIdGenerator(),
        time,
        idGeneratorContainer.entityIdGenerator(),
        pillarPositionRadius * Complex.rotation(angle + math.Pi / 6),
        pillarShape.vertices.map(_ * Complex.rotation(angle + math.Pi / 6 + math.Pi))
      )

      pillarAction

    }

    val outerPolygon = Shape
      .regularPolygon(sizeNumber, roomRadius + 15)
    val innerPolygon = Shape.regularPolygon(sizeNumber, roomRadius)

    CreateObstacle(
      idGeneratorContainer.gameActionIdGenerator(),
      time,
      idGeneratorContainer.entityIdGenerator(),
      0,
      (outerPolygon.vertices :+ outerPolygon.vertices.head) ++ (innerPolygon.vertices :+ innerPolygon.vertices.head).reverse
    ) +:
      (0 to sizeNumber).toList.map(pillar)
  }

  def playersStartingPosition: Complex = 0

  def name: String = "Boss 103"
}
