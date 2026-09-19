package gamelogic.entities.boss.boss104

import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{
  Entity,
  LivingEntity,
  MovingBody,
  Resource,
  WithAbilities,
  WithTarget,
  WithThreat
}
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Shape}

/** The "add" (additional unit) that Boss104 summons with
  * [[gamelogic.abilities.boss.boss104.SpawnBigGuy]].
  *
  * It illustrates the full set of capabilities an add can have, each brought in by one trait:
  *   - [[MovingBody]]: it has a position and can move
  *   - [[LivingEntity]]: it has life and can die
  *   - [[WithThreat]]: it keeps a threat table (fed by the `DamageThreatAware` buff that
  *     [[gamelogic.gamestate.gameactions.boss104.AddBigGuy]] puts on it at spawn)
  *   - [[WithTarget]]: it has a current target
  *   - [[WithAbilities]]: it can cast, here an auto attack and the interruptible
  *     [[gamelogic.abilities.boss.boss104.BigGuyKick]]
  *
  * A simpler add only needs a subset of those. Its brain lives in
  * `application.ai.boss.boss104units.BigGuyController` (in the game-server sub-project).
  */
final case class BigGuy(
    id: Entity.Id,
    time: Long,
    pos: Complex,
    direction: Angle,
    rotation: Angle,
    speed: Double,
    moving: Boolean,
    life: Double,
    damageThreats: Map[Id, ThreatAmount],
    targetId: Entity.Id,
    relevantUsedAbilities: Map[AbilityId, Ability]
) extends MovingBody
    with LivingEntity
    with WithThreat
    with WithTarget
    with WithAbilities {
  override def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): BigGuy = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  override def maxLife: Double = BigGuy.maxLife

  override def canBeStunned: Boolean = true

  override protected def patchLifeTotal(newLife: ThreatAmount): BigGuy = copy(life = newLife)

  override def healingThreats: Map[Id, ThreatAmount] = Map() // don't care about healing threat

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): BigGuy = copy(
    damageThreats = damageThreats + (threatId -> damageThreats.get(threatId).fold(delta)(_ + delta))
  )

  override def changeHealingThreats(threatId: Id, delta: ThreatAmount): BigGuy = this

  override def changeTarget(newTargetId: Id): BigGuy = copy(targetId = newTargetId)

  override def abilities: Set[AbilityId] = Set(Ability.autoAttackId, Ability.boss104BigGuyKick)

  override def useAbility(ability: Ability): BigGuy = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  override def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  override def maxResourceAmount: ThreatAmount = 0.0

  override protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): BigGuy =
    this

  override def shape: ConvexPolygon = BigGuy.shape

  override def teamId: TeamId = Entity.teams.mobTeam

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        BigGuy.damageOnTick,
        BigGuy.tickRate,
        NoResource,
        BigGuy.range * 2
      )
    ).filter(canUseAbilityBoolean(_, time, gameState))
}

object BigGuy {
  // The three numbers that really characterise an add: how much life it has, how big it is, and
  // (through `abilities` above) what it can do. Everything else is boilerplate.
  inline def maxLife: Double = 800.0

  val shape: ConvexPolygon = Shape.regularPolygon(3, 2 * Constants.playerRadius)
  val fullSpeed: Double    = Constants.playerSpeed * 2 / 5
  val range: Double        = shape.radius + Constants.playerRadius
  val damageOnTick         = 3.0
  val tickRate             = 1200L
}
