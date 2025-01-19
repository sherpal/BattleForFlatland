package gamelogic.entities.boss

import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss101.{BigDot, BigHit, SmallHit}
import gamelogic.entities.Entity
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

/** Very first boss to be coded. Probably not the most exiting one but the goal was to have a first
  * proof of concept and retrieve as much feedback as possible.
  *
  * The abilities of the boss are the following:
  *   - "big" hit that directly attack the target (with a casting time so that the player can
  *     react). This attack will probably kill any player other than a tank (under cd)
  *   - dot placed on someone different from the target (no casting time)
  *   - spawn adds which will move towards and attack the player with the biggest healing threat
  *     (the heal if he is the only one). These adds will have melee attacks and move not too fast,
  *     leaving time to dps to kill them before they reach the heal.
  *
  * This boss is intended for 5 players (1 tank, 2 dps and 2 healers)
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
    healingThreats: Map[Entity.Id, ThreatAmount],
    damageThreats: Map[Entity.Id, ThreatAmount]
) extends BossEntity {

  def name: String = Boss101.name

  def shape: Circle = Boss101.shape

  def abilities: Set[AbilityId] =
    Set(Ability.boss101BigDotId, Ability.boss101BigHitId, Ability.boss101SmallHitId)

  def useAbility(ability: Ability): Boss101 = copy(
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
  ): Boss101 =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  protected def patchLifeTotal(newLife: Double): Boss101 =
    copy(life = newLife)

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def changeDamageThreats(threatId: Entity.Id, delta: ThreatAmount): Boss101 =
    copy(damageThreats =
      damageThreats + (threatId -> (damageThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeHealingThreats(threatId: Entity.Id, delta: ThreatAmount): Boss101 =
    copy(healingThreats =
      healingThreats + (threatId -> (healingThreats.getOrElse(threatId, 0.0) + delta))
    )

  def changeTarget(newTargetId: Entity.Id): Boss101 = copy(targetId = newTargetId)

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): Boss101 = this

  def abilityNames: Map[AbilityId, String] = Map(
    Ability.boss101BigHitId   -> BigHit.name,
    Ability.boss101SmallHitId -> SmallHit.name,
    Ability.boss101BigDotId   -> BigDot.name
  )
}

object Boss101 extends BossFactory[Boss101] with BossMetadata {

  def intendedFor: Int = 5

  def maybeAIComposition: Option[List[PlayerClasses]] =
    Some(
      List(
        PlayerClasses.Hexagon,
        PlayerClasses.Triangle,
        PlayerClasses.Square,
        PlayerClasses.Pentagon,
        PlayerClasses.Pentagon
      )
    )

  final val shape: Circle = new Circle(Constants.bossRadius)

  final val maxLife: Double = 20000

  final val meleeRange: Distance = shape.radius + 20.0
  final val rangeRange: Distance = 2000.0 // basically infinite distance

  final val name: String = "Boss 101"

  final val fullSpeed: Double = 300.0

  def initialBoss(entityId: Entity.Id, time: Long): Boss101 =
    Pointed[Boss101].unit
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
      entityId: Entity.Id,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    healAndDamageAwareActions(entityId, time)

  def stagingBossActions(time: Long)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(
      CreateObstacle(
        GameAction.Id.zero,
        time,
        genEntityId(),
        Complex(0, 200),
        Shape.regularPolygon(4, 50).vertices
      )
    )

  def whenBossDiesActions(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameState.allBuffs.collect {
      case bigDot: Buff if bigDot.resourceIdentifier == Buff.boss101BigDotIdentifier =>
        RemoveBuff(genActionId(), time, bigDot.bearerId, bigDot.buffId)
    }.toVector

  def playersStartingPosition: Complex = -100 * Complex.i
}
