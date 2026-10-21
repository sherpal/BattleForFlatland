package gamelogic.entities.boss.boss105

import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.buffs.boss.boss105.Yearning
import gamelogic.abilities.Ability.AbilityId
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.NoResource
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.classes.Constants
import gamelogic.entities.{
  Entity,
  LivingEntity,
  MovingBody,
  Resource,
  WithAbilities,
  WithTarget,
  WithThreat
}
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Shape}

/** The Shade is the twin of Boss105: an add that fights next to it for as long as it lives, and
  * that comes back some time after having been killed (see
  * [[gamelogic.buffs.boss.boss105.Mourning]]).
  *
  * It is a regular add (see [[gamelogic.entities.boss.boss104.BigGuy]] for a commented one): it
  * attacks the player with the biggest threat, auto-attacks it, and casts the interruptible
  * [[gamelogic.abilities.boss.boss105.Siphon]]. What makes it special is not in this class but in
  * its relationship with the boss: its [[gamelogic.buffs.boss.boss105.ShadeBond]] buff tells the
  * boss when it dies, and the boss pulses with [[gamelogic.abilities.boss.boss105.Resonance]] when
  * the two of them get too close.
  */
final case class Shade(
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
  ): Shade = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  override def maxLife: Double = Shade.maxLife

  override def canBeStunned: Boolean = true

  override protected def patchLifeTotal(newLife: Double): Shade = copy(life = newLife)

  override def healingThreats: Map[Id, ThreatAmount] = Map() // healing threat is not used

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): Shade =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  override def changeHealingThreats(threatId: Id, delta: ThreatAmount): Shade = this

  override def changeTarget(newTargetId: Id): Shade = copy(targetId = newTargetId)

  override def abilities: Set[AbilityId] = Set(Ability.autoAttackId, Ability.boss105ShadeSiphon)

  override def useAbility(ability: Ability): Shade =
    copy(relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability))

  override def resourceAmount: Resource.ResourceAmount = Resource.ResourceAmount(0.0, NoResource)

  override def maxResourceAmount: Double = 0.0

  override protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): Shade =
    this

  override def shape: ConvexPolygon = Shade.shape

  override def teamId: TeamId = Entity.teams.mobTeam

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        Shade.autoAttackDamage,
        Shade.autoAttackTickRate,
        NoResource,
        Shade.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbilityBoolean(_, time, gameState))
}

object Shade {

  inline def maxLife: Double = 6000.0

  /** A big triangle, the lowest rank in Flatland: the shadow of a circle is a humble shape. */
  val shape: ConvexPolygon = Shape.regularPolygon(3, 3 * Constants.playerRadius)

  val fullSpeed: Double = Constants.playerSpeed * 4 / 5

  /** How fast it goes when the boss calls it. */
  val yearningSpeed: Double = fullSpeed * Yearning.speedMultiplier

  /** Distance between the centres below which the Shade can hit its target. */
  val meleeRange: Double = shape.radius + Constants.playerRadius + 5

  inline def autoAttackDamage: Double = 3.0
  inline def autoAttackTickRate: Long = 1200L

  /** Where the Shade stands when the fight begins, and where it comes back after having fallen: in
    * the corner of the room which is the farthest from the boss, so that the twins always start
    * apart from each other.
    *
    * @param roomHalfSize
    *   half the side of the (square) room
    * @param bossPosition
    *   where the boss stands when the Shade falls
    */
  def reformationPosition(roomHalfSize: Double, bossPosition: Complex): Complex =
    reformationCorners(roomHalfSize)
      .maxBy(_.distanceTo(bossPosition))(using Ordering.Double.TotalOrdering)

  /** The four places where the Shade may start the fight or come back, in the corners of the room.
    */
  def reformationCorners(roomHalfSize: Double): Vector[Complex] = {
    val c = roomHalfSize * 0.72
    Vector(Complex(c, -c), Complex(c, c), Complex(-c, c), Complex(-c, -c))
  }

}
