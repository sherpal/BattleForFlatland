package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Siphon, Skewer, VolatileMark}
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.buffs.boss.boss105.{Mourning, TwinBond, VolatileCharge, Yearning}
import gamelogic.docs.{formatNumber, formatPeriod, formatSeconds, AbilityDoc, BossMetadata}
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.boss105.{ReformationPoint, Shade}
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.*
import gamelogic.gamestate.gameactions.boss105.AddShade
import gamelogic.gamestate.gameactions.{CreateObstacle, RemoveBuff, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses
import models.syntax.Pointed

/** Boss105: a high priest and its [[gamelogic.entities.boss.boss105.Shade]].
  *
  * It is the first boss designed for a team with two tanks: Squares, Squares everywhere.
  *   - While its Shade lives, the boss is bonded to it ([[gamelogic.buffs.boss.boss105.TwinBond]])
  *     and takes only a fraction of the damage it should. One tank holds the boss, the other one
  *     the Shade, and the team must kill the Shade.
  *   - The twins must stay away from each other, otherwise they pulse with
  *     [[gamelogic.abilities.boss.boss105.Resonance]], hurting everyone.
  *   - When the Shade falls, the boss is exposed and frenzied
  *     ([[gamelogic.buffs.boss.boss105.Mourning]]), until the Shade comes back, in the corner of
  *     the room which is the farthest from the boss.
  *   - In any case, the boss fires lines ([[gamelogic.abilities.boss.boss105.Skewer]]) and puts
  *     explosive charges on players ([[gamelogic.abilities.boss.boss105.VolatileMark]]).
  */
final case class Boss105(
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

  override protected def patchLifeTotal(newLife: Double): LivingEntity = copy(life = newLife)

  override def shape: Circle = Boss105.shape

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  override protected def patchResourceAmount(newResourceAmount: ResourceAmount): WithAbilities =
    this

  override def abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId        -> "Auto Attack",
    Ability.boss105Resonance    -> Resonance.name,
    Ability.boss105Reunion      -> Reunion.name,
    Ability.boss105Skewer       -> Skewer.name,
    Ability.boss105VolatileMark -> VolatileMark.name
  )

  override def abilities: Set[AbilityId] = abilityNames.keySet

  override def teamId: TeamId = Entity.teams.mobTeam

  override def resourceAmount: ResourceAmount = ResourceAmount(0, NoResource)

  override def useAbility(ability: Ability): WithAbilities = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability)
  )

  override def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): MovingBody = copy(
    time = time,
    pos = position,
    direction = direction,
    rotation = rotation,
    speed = speed,
    moving = moving
  )

  override def changeTarget(newTargetId: Id): WithTarget = copy(targetId = newTargetId)

  override def changeHealingThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(healingThreats = healingThreats.addAmount(threatId, delta))

  override def maxResourceAmount: Double = 0.0

  override def name: String = Boss105.name

  /** Whether the Shade is dead and the boss is exposed and frenzied. */
  def isMourning(gameState: GameState): Boolean = gameState.hasBuffOfType[Mourning](id)

  /** Candidate auto attack for this instant, if the boss is allowed to use it right now. It hits
    * harder and faster while the boss is [[Mourning]].
    */
  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] = {
    val frenzied = isMourning(gameState)
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        if frenzied then Boss105.frenziedAutoAttackDamage else Boss105.autoAttackDamage,
        if frenzied then Boss105.frenziedAutoAttackTickRate else Boss105.autoAttackTickRate,
        NoResource,
        Boss105.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbilityBoolean(_, time, gameState))
  }

}

object Boss105 extends BossFactory[Boss105] with BossMetadata {

  val name: String = "Boss 105"

  override def intendedFor: Int = 5

  /** Two tanks, one for each twin. */
  override def maybeAIComposition: Option[List[PlayerClasses]] = Some(
    List(
      PlayerClasses.Square,
      PlayerClasses.Square,
      PlayerClasses.Triangle,
      PlayerClasses.Pentagon,
      PlayerClasses.Hexagon
    )
  )

  inline def shape: Circle = Boss101.shape

  inline def meleeRange: Distance = shape.radius + 20.0

  inline def fullSpeed: Double = 220.0

  inline def autoAttackDamage: Double = 4.0
  inline def autoAttackTickRate: Long = 1000L

  inline def frenziedAutoAttackDamage: Double = 7.0
  inline def frenziedAutoAttackTickRate: Long = 900L

  inline def maxLife: Double = 11000

  /** Where the boss stands when the fight begins. */
  val startingPosition: Complex = Complex(-250, 0)

  override def initialBoss(entityId: Id, time: Long): Boss105 = Pointed[Boss105].unit.copy(
    id = entityId,
    time = time,
    pos = startingPosition,
    maxLife = maxLife,
    life = maxLife,
    speed = fullSpeed,
    relevantUsedAbilities = Map(
      Ability.boss105Resonance -> Pointed[Resonance].unit.copy(
        time = Resonance.setBeginningOfGameAbilityUse(time)
      ),
      Ability.boss105Reunion -> Pointed[Reunion].unit.copy(
        time = Reunion.setBeginningOfGameAbilityUse(time)
      ),
      Ability.boss105Skewer -> Pointed[Skewer].unit.copy(
        time = Skewer.setBeginningOfGameAbilityUse(time)
      ),
      Ability.boss105VolatileMark -> Pointed[VolatileMark].unit.copy(
        time = VolatileMark.setBeginningOfGameAbilityUse(time)
      )
    )
  )

  /** Where the Shade starts the fight. */
  def shadeStartingPosition: Complex = Shade.reformationPosition(size, startingPosition)

  /** The boss, and its Shade, are there from the very beginning. */
  override def initialBossActions(entityId: Id, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    healAndDamageAwareActions(entityId, time) :+ AddShade(
      genActionId(),
      time,
      genEntityId(),
      shadeStartingPosition,
      entityId,
      genBuffId(),
      genBuffId(),
      genBuffId()
    )

  /** Nobody should die after the victory: the Shade goes away, and so do the charges. */
  override def whenBossDiesActions(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] =
    gameState.allTEntities[Shade].keys.map(RemoveEntity(genActionId(), time, _)).toVector ++
      gameState
        .allTEntities[ReformationPoint]
        .keys
        .map(RemoveEntity(genActionId(), time, _)) ++
      gameState
        .allTBuffs[VolatileCharge]
        .map(buff => RemoveBuff(genActionId(), time, buff.bearerId, buff.buffId))

  /** Half of the side of the (square) room. */
  val size = 420.0

  def gameBoundariesActions(
      time: Long
  )(using IdGeneratorContainer): Vector[CreateObstacle] = Obstacle.squareGameArea(time, size)

  override def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = gameBoundariesActions(time)

  override def playersStartingPosition: Complex = 0

  def description: String =
    "A high priest fights side by side with its Shade, and it takes the blows for it: while the " ++
      s"Shade lives, the boss takes ${formatNumber((1 - TwinBond.damageMultiplier) * 100)}% less " ++
      "damage. A tank must hold each twin, and keep them far apart from each other, because " ++
      "they hurt everyone when they get too close. When the Shade falls, the boss is exposed " ++
      s"and frenzied for ${formatSeconds(Mourning.duration)}, until the Shade comes back in the " ++
      "corner of the room which is the farthest from the boss. Meanwhile, beware of the lines " ++
      "that the boss fires, and of the charges that it puts on players."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(autoAttackDamage)} damage to its target. While the " ++
        s"boss is exposed, it deals ${formatNumber(frenziedAutoAttackDamage)} damage every " ++
        s"${formatPeriod(frenziedAutoAttackTickRate)}.",
      autoAttackTickRate,
      0L
    ),
    AbilityDoc.fromMetadata(
      Resonance,
      s"Only used when the Shade stands within ${formatNumber(Resonance.range)} units of the " ++
        s"boss. Deals ${formatNumber(Resonance.damage)} damage to every player, again and " ++
        "again, until the twins are pulled apart."
    ),
    AbilityDoc.fromMetadata(
      Reunion,
      "Calls the Shade, which rushes towards the boss at " ++
        s"${formatNumber(Yearning.speedMultiplier)} times its speed, ignoring everything else, " ++
        s"for ${formatSeconds(Yearning.duration)}. Then the Shade goes back to its target. A " ++
        "stunned Shade does not move."
    ),
    AbilityDoc.fromMetadata(
      Skewer,
      "Fires a beam from the boss, through a random player (other than its target), up to the " ++
        s"walls of the room. Players standing on the beam when it is fired take " ++
        s"${formatNumber(Skewer.damage)} damage. The beam follows its target until the end of " ++
        "the cast."
    ),
    AbilityDoc.fromMetadata(
      VolatileMark,
      s"Puts a volatile charge on ${VolatileMark.numberOfBearers} random players. After " ++
        s"${formatSeconds(VolatileCharge.duration)}, the charge explodes and deals " ++
        s"${formatNumber(VolatileCharge.damage)} damage to every player within " ++
        s"${formatNumber(VolatileCharge.radius)} units of its bearer, the bearer included. " ++
        "Can be dispelled, in which case it is harmless."
    ),
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(Shade.autoAttackDamage)} damage to its target.",
      Shade.autoAttackTickRate,
      0L
    ).castBy("Shade"),
    AbilityDoc(
      Ability.boss105ShadeSiphon,
      "Siphon",
      s"Drains ${formatNumber(Siphon.damage)} life points from a player (other than its " ++
        "target, if possible), and heals the boss for " ++
        s"${formatNumber(Siphon.healFraction * 100)}% of its maximum life. Can be interrupted " ++
        "(with the Triangle's Cut, for example).",
      Siphon.cooldown,
      Siphon.castingTime
    ).castBy("Shade")
  )
}
