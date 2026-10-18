package gamelogic.entities.boss.firstraid

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.sirofvice.{CleansingNova, HolyFlame, HolyWrath, Punishment}
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.buffs.boss.sirofvice.Inflamed
import gamelogic.abilities.boss.sirofvice.SacredGround
import gamelogic.buffs.boss.sirofvice.{Punished, Purified}
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds, AbilityDoc, BossMetadata}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.classes.Constants
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{CreateObstacle, RemoveBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses
import models.syntax.Pointed

/** First boss of the first raid, freely inspired by the Maiden of Virtue.
  *
  * The fight takes place in a hexagonal hall with six pillars. The boss has five abilities:
  *   - [[gamelogic.abilities.boss.sirofvice.SacredGround]]: an aura that hurts and silences (for
  *     mana abilities) every player too close to the boss. Melee players live in it, the others
  *     must stay away from it.
  *   - [[gamelogic.abilities.boss.sirofvice.HolyFlame]]: a dispellable damage over time on a random
  *     player.
  *   - [[gamelogic.abilities.boss.sirofvice.HolyWrath]]: hits a random player, then jumps from
  *     player to player as long as they are close to each other, harder at each jump. Players must
  *     spread.
  *   - [[gamelogic.abilities.boss.sirofvice.Punishment]]: every player is frozen until they take
  *     damage. Healers typically step into the aura right before it, so that it frees them.
  *   - [[gamelogic.abilities.boss.sirofvice.CleansingNova]]: deadly damage to every player in sight
  *     of the boss, which forces everyone to hide behind a pillar.
  */
final case class SirOfVice(
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

  def name: String = SirOfVice.name

  def shape: Circle = SirOfVice.shape

  def abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId           -> "Auto attack",
    Ability.sirOfViceCleansingNova -> CleansingNova.name,
    Ability.sirOfVicePunishment    -> Punishment.name,
    Ability.sirOfViceSacredGround  -> "Sacred Ground",
    Ability.sirOfViceHolyFlame     -> HolyFlame.name,
    Ability.sirOfViceHolyWrath     -> HolyWrath.name
  )

  def changeTarget(newTargetId: Entity.Id): SirOfVice = copy(targetId = newTargetId)

  def changeDamageThreats(threatId: Entity.Id, delta: ThreatAmount): SirOfVice =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  def changeHealingThreats(threatId: Entity.Id, delta: ThreatAmount): SirOfVice =
    copy(healingThreats = healingThreats.addAmount(threatId, delta))

  protected def patchLifeTotal(newLife: Double): SirOfVice = copy(life = newLife)

  def abilities: Set[AbilityId] = abilityNames.keySet

  def useAbility(ability: Ability): SirOfVice = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  def maxResourceAmount: Double      = 0.0
  def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  protected def patchResourceAmount(newResourceAmount: Resource.ResourceAmount): SirOfVice = this

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): SirOfVice =
    copy(
      time = time,
      pos = position,
      direction = direction,
      rotation = rotation,
      speed = speed,
      moving = moving
    )

  def teamId: Entity.TeamId = Entity.teams.mobTeam

  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        SirOfVice.autoAttackDamage,
        SirOfVice.autoAttackTickRate,
        NoResource,
        SirOfVice.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbilityBoolean(_, time, gameState))

}

object SirOfVice extends BossFactory[SirOfVice] with BossMetadata {

  val name: String = "Sir of Vice"

  def intendedFor: Int = 5

  def maybeAIComposition: Option[List[PlayerClasses]] = Some(
    List(
      PlayerClasses.Square,
      PlayerClasses.Triangle,
      PlayerClasses.Pentagon,
      PlayerClasses.Pentagon,
      PlayerClasses.Hexagon
    )
  )

  inline def shape: Circle = Boss101.shape

  inline def meleeRange: Distance = shape.radius + 20.0

  inline def fullSpeed: Double = 250.0

  inline def autoAttackDamage: Double = 5.0
  inline def autoAttackTickRate: Long = 1000L

  inline def maxLife: Double = 30000

  def initialBoss(entityId: Entity.Id, time: Long): SirOfVice = Pointed[SirOfVice].unit.copy(
    id = entityId,
    time = time,
    maxLife = maxLife,
    life = maxLife,
    speed = fullSpeed,
    relevantUsedAbilities = Map(
      Ability.sirOfViceCleansingNova -> Pointed[CleansingNova].unit.copy(
        time = CleansingNova.setBeginningOfGameAbilityUse(time)
      ),
      Ability.sirOfVicePunishment -> Pointed[Punishment].unit.copy(
        time = Punishment.setBeginningOfGameAbilityUse(time)
      ),
      Ability.sirOfViceHolyFlame -> Pointed[HolyFlame].unit.copy(
        time = HolyFlame.setBeginningOfGameAbilityUse(time)
      ),
      Ability.sirOfViceHolyWrath -> Pointed[HolyWrath].unit.copy(
        time = HolyWrath.setBeginningOfGameAbilityUse(time)
      )
    )
  )

  def initialBossActions(
      entityId: Entity.Id,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    healAndDamageAwareActions(entityId, time)

  /** Removes the remaining [[Inflamed]] debuffs, so that nobody dies after the victory. */
  def whenBossDiesActions(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameState
      .allTBuffs[Inflamed]
      .map(buff => RemoveBuff(genActionId(), time, buff.bearerId, buff.buffId))
      .toVector

  /** Players arrive from the left of the hall, between two pillars. The boss waits at its centre.
    */
  def playersStartingPosition: Complex = Complex(-300, 0)

  // ---------------------------------------------------------------------------------------------
  // The room
  // ---------------------------------------------------------------------------------------------

  /** Distance from the centre of the hall to the corners of its (hexagonal) walls. */
  inline def roomRadius: Double = 400.0

  inline def wallThickness: Double = 15.0

  /** Distance from the centre of the hall to the centre of the walls, on the inside. */
  inline def roomInnerApothem: Double = roomRadius * 0.8660254037844386 - wallThickness / 2

  inline def pillarPositionRadius: Double = 240.0
  inline def pillarRadius: Double         = Constants.playerRadius * 3
  inline def numberOfPillars: Int         = 6

  /** Corners of the hall. */
  val roomVertices: Vector[Complex] = Shape.regularPolygon(numberOfPillars, roomRadius).vertices

  /** Pillars stand in front of the middle of each wall (hence the `Pi / 6` shift with the corners
    * of the room).
    */
  val pillarPositions: Vector[Complex] = (0 until numberOfPillars).toVector.map(index =>
    Complex.polar(pillarPositionRadius, pillarAngle(index))
  )

  private def pillarAngle(index: Int): Angle =
    Angle.Tau * Angle.fromDouble(index.toDouble / numberOfPillars) + Angle.Pi / 6

  /** Pillars are triangles with a corner pointing towards the centre of the hall. Vertices are
    * relative to the pillar position.
    */
  def pillarVertices(index: Int): Vector[Complex] =
    Shape
      .regularPolygon(3, pillarRadius)
      .vertices
      .map(_ * Complex.rotation(pillarAngle(index) + Angle.Pi))

  /** Where to stand in order to be hidden by the pillar at `pillarPosition` from someone standing
    * at `from`: right behind it, on the line going from `from` through the centre of the pillar.
    */
  def hidingSpotBehind(pillarPosition: Complex, from: Complex): Complex =
    pillarPosition + (pillarPosition - from).safeNormalized * (pillarRadius + Constants.playerRadius + 10)

  def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = {
    // walls are slightly extended on both sides so that they overlap at the corners
    val extension = wallThickness / roomRadius
    val walls = roomVertices.zip(roomVertices.tail :+ roomVertices.head).map { (z1, z2) =>
      CreateObstacle(
        genActionId(),
        time,
        genEntityId(),
        0,
        Obstacle.segmentObstacleVertices(
          z1 + extension * (z1 - z2),
          z2 + extension * (z2 - z1),
          wallThickness
        )
      )
    }

    val pillars = pillarPositions.zipWithIndex.map { (position, index) =>
      CreateObstacle(genActionId(), time, genEntityId(), position, pillarVertices(index))
    }

    walls ++ pillars
  }


  def description: String =
    "First boss of the first raid. The fight takes place in a hexagonal hall with six pillars. " ++
      "Melee players fight within the boss' aura, while everybody else must stay out of it. " ++
      "Spread out, get ready to be frozen, and hide behind a pillar when the boss unleashes its " ++
      "Cleansing Nova."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(autoAttackDamage)} damage to its target.",
      autoAttackTickRate,
      0L
    ),
    AbilityDoc.fromMetadata(
      SacredGround,
      s"Aura hitting all players within ${formatNumber(SacredGround.radius)} units of the boss. " ++
        s"The ${formatNumber(SacredGround.damage)} damage is split between them (the more players " ++
        "in it, the less each one takes), and they are silenced for " ++
        s"${formatSeconds(Purified.duration)}: they can't use abilities costing mana."
    ),
    AbilityDoc.fromMetadata(
      HolyFlame,
      "Sets a random player other than its target on fire, dealing increasing damage every " ++
        s"${formatPeriod(Inflamed.tickRate)} (${formatNumber(Inflamed.baseDamage)}, then " ++
        s"${formatNumber(2 * Inflamed.baseDamage)}, and so on) for " ++
        s"${formatSeconds(Inflamed.duration)}. Can be dispelled."
    ),
    AbilityDoc.fromMetadata(
      HolyWrath,
      s"Hits a random player for ${formatNumber(HolyWrath.baseDamage)} damage, then jumps to the " ++
        s"closest player within ${formatNumber(HolyWrath.jumpRange)} units not hit yet, and so on. " ++
        s"Each jump deals ${formatNumber(HolyWrath.damageMultiplier)} times more damage than the " ++
        "previous one: spread out!"
    ),
    AbilityDoc.fromMetadata(
      Punishment,
      "Interrupts all players, and freezes them until they take damage (for at most " ++
        s"${formatSeconds(Punished.duration)}). Healers typically step into the aura right " ++
        "before, so that it frees them."
    ),
    AbilityDoc.fromMetadata(
      CleansingNova,
      s"Deals ${formatNumber(CleansingNova.damage)} damage to every player in sight of the boss. " ++
        "Hide behind a pillar!"
    )
  )
}
