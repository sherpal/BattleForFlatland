package gamelogic.entities.boss

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss101.{BigDot, BigHit, SmallHit}
import gamelogic.entities.Entity
import gamelogic.entities.Entity.Id
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.syntax.Pointed
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.buffs.Buff
import gamelogic.docs.BossMetadata
import models.bff.outofgame.PlayerClasses

/** This is a clone of the [[Boss101]] with low life to test how the end of a boss behaves in
  * general.
  */
final case class Boss101Dev(
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

  def name: String = Boss101Dev.name

  def shape: Circle = Boss101Dev.shape

  def abilities: Set[AbilityId] =
    Set(Ability.boss101BigDotId, Ability.boss101BigHitId, Ability.boss101SmallHitId)

  def useAbility(ability: Ability): Boss101Dev = copy(
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
  ): Boss101Dev =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  protected def patchLifeTotal(newLife: Double): Boss101Dev =
    copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def changeDamageThreats(threatId: Id, delta: ThreatAmount): Boss101Dev =
    copy(damageThreats =
      damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeHealingThreats(threatId: Id, delta: ThreatAmount): Boss101Dev =
    copy(healingThreats =
      healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeTarget(newTargetId: Id): Boss101Dev = copy(targetId = newTargetId)

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Boss101Dev = this

  def abilityNames: Map[AbilityId, String] = Map(
    Ability.boss101BigHitId   -> BigHit.name,
    Ability.boss101SmallHitId -> SmallHit.name,
    Ability.boss101BigDotId   -> BigDot.name
  )
}

object Boss101Dev extends BossFactory[Boss101Dev] with BossMetadata {

  override def intendedFor: Int = 5

  override def maybeAIComposition: Option[List[PlayerClasses]] = None

  inline def shape: Circle = Circle(Constants.bossRadius)

  inline def maxLife: Double = 400 // 20000

  inline def meleeRange: Distance = shape.radius + 20.0
  inline def rangeRange: Distance = 2000.0 // basically infinite distance

  val name: String = "Boss 101 Dev"

  inline def fullSpeed: Double = 300.0

  def initialBoss(entityId: Entity.Id, time: Long): Boss101Dev =
    Pointed[Boss101Dev].unit
      .copy(
        id = entityId,
        time = time,
        speed = fullSpeed,
        relevantUsedAbilities = Map(
          Ability.boss101BigDotId -> Pointed[BigDot].unit.copy(
            time = time - BigDot.cooldown + BigDot.timeToFirstBigDot
          ),
          Ability.boss101BigHitId -> Pointed[BigHit].unit.copy(
            time = time - BigHit.cooldown + BigHit.timeToFirstBigHit
          )
        ),
        maxLife = maxLife,
        life = maxLife
      )

  def initialBossActions(
      entityId: Id,
      time: Long,
      idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    healAndDamageAwareActions(entityId, time, idGeneratorContainer)

  def stagingBossActions(time: Id, idGeneratorContainer: IdGeneratorContainer): List[GameAction] =
    List(
      CreateObstacle(
        0L,
        time,
        idGeneratorContainer.entityIdGenerator(),
        Complex(0, 200),
        Shape.regularPolygon(4, 50).vertices
      )
    )

  def whenBossDiesActions(
      gameState: GameState,
      time: Long,
      idGeneratorContainer: IdGeneratorContainer
  ): List[GameAction] =
    gameState.allBuffs.collect {
      case bigDot: Buff if bigDot.resourceIdentifier == Buff.boss101BigDotIdentifier =>
        RemoveBuff(
          idGeneratorContainer.gameActionIdGenerator(),
          time,
          bigDot.bearerId,
          bigDot.buffId
        )
    }.toList

  def playersStartingPosition: Complex = -100 * Complex.i
}
