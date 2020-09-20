package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss103.{CleansingNova, HolyFlame, Punishment}
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.docs.BossMetadata
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.dawnoftime.Boss102.fullSpeed
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.classes.Constants
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.gamestate.{GameAction, GameState}
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

  def shape: Circle = Boss103.shape

  def abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId -> "Auto attack",
    Ability.boss103CleansingNovaId -> "Cleansing Nova",
    Ability.boss103PunishmentId -> "Punishment",
    Ability.boss103SacredGroundId -> "Sacred Ground",
    Ability.boss103HolyFlameId -> "Holy Flame"
  )

  def changeTarget(newTargetId: Id): Boss103 = copy(targetId = newTargetId)

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(damageThreats = damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta)))

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss103 =
    copy(healingThreats = healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta)))

  protected def patchLifeTotal(newLife: Double): Boss103 = copy(life = newLife)

  def abilities: Set[AbilityId] = Set(
    Ability.autoAttackId,
    Ability.boss103CleansingNovaId,
    Ability.boss103PunishmentId,
    Ability.boss103SacredGroundId,
    Ability.boss103HolyFlameId
  )

  def useAbility(ability: Ability): Boss103 = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): Boss103 = this

  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): Boss103 =
    copy(time = time, pos = position, direction = direction, rotation = rotation, speed = speed, moving = moving)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        0L,
        time,
        id,
        targetId,
        Boss103.autoAttackDamage,
        Boss103.autoAttackTickRate,
        NoResource,
        Boss103.meleeRange
      )
    ).filter(_.canBeCast(gameState, time)).filter(canUseAbility(_, time))

}

object Boss103 extends BossFactory[Boss103] with BossMetadata {

  final val shape: Circle = Boss101.shape

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  final val autoAttackDamage: Double = 5.0
  final val autoAttackTickRate: Long = 1000L

  final val maxLife: Double = 30000
  def initialBoss(entityId: Id, time: Id): Boss103 = Pointed[Boss103].unit.copy(
    id      = entityId,
    time    = time,
    maxLife = maxLife,
    life    = maxLife,
    speed   = fullSpeed,
    relevantUsedAbilities = Map(
      Ability.boss103CleansingNovaId -> Pointed[CleansingNova].unit.copy(
        time = time - CleansingNova.cooldown + CleansingNova.timeToFirstAbility
      ),
      Ability.boss103PunishmentId -> Pointed[Punishment].unit.copy(
        time = time - Punishment.cooldown + Punishment.timeToFirstAbility
      ),
      Ability.boss103HolyFlameId -> Pointed[HolyFlame].unit.copy(
        time = time - HolyFlame.cooldown + HolyFlame.timeToFirstAbility
      )
    )
  )

  def initialBossActions(entityId: Id, time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    healAndDamageAwareActions(entityId, time, idGeneratorContainer)

  final val roomRadius: Double           = 400.0
  final val pillarPositionRadius: Double = roomRadius * 6 / 10
  final val pillarRadius: Double         = Constants.playerRadius * 3
  final val sizeNumber: Int              = 6

  def stagingBossActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] = {
    val pillarShape = Shape.regularPolygon(3, pillarRadius)
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

  val name: String = "Boss 103"

  def intendedFor: AbilityId = 5
}
