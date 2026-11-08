package gamelogic.entities.boss.firstraid

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.spiritofnara.*
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.buffs.boss.spiritofnara.*
import gamelogic.docs.{formatNumber, formatSeconds, AbilityDoc, BossMetadata}
import gamelogic.entities.Resource.{Mana, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.spiritofnara.{BlizzardCloud, FlameWreathRing, WaterElemental}
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.{CreateObstacle, RemoveBuff, RemoveEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Circle, Shape}
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses
import models.syntax.Pointed

/** Second boss of the first raid, freely inspired by the Shade of Aran.
  *
  * The Spirit of Nara never moves from the centre of its circular library. It is a caster with a
  * mana pool, and it does not need a tank so much as a group that knows where to stand:
  *   - it throws [[Fireball]]s, [[Frostbolt]]s and [[ArcaneMissiles]] at random players. Each of
  *     them can be cut, which locks it out of that school for a while (see [[SchoolLockout]]);
  *   - [[Counterspell]] keeps the casters out of its reach, and [[ChainsOfIce]] roots someone;
  *   - every [[SpiritOfNara.specialInterval]], one of its three special abilities, never twice the
  *     same in a row: the [[ArcaneExplosion]] (preceded by a [[MagneticPull]] that brings everyone
  *     close and slows them down), the [[FlameWreath]] (nobody may move across the rings), and the
  *     [[Blizzard]] (a storm going around the room);
  *   - when its mana runs low, it polymorphs everyone and drinks ([[MassPolymorph]]), then hits
  *     everyone with a [[Pyroblast]];
  *   - under 40%, then under 20% of its life, it summons Water Elementals
  *     ([[SummonWaterElementals]]);
  *   - after 8 minutes, it goes berserk ([[ShadowPyroblast]]).
  */
final case class SpiritOfNara(
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
    resourceAmount: ResourceAmount,
    relevantUsedAbilities: Map[AbilityId, Ability],
    healingThreats: Map[Entity.Id, ThreatAmount],
    damageThreats: Map[Entity.Id, ThreatAmount]
) extends BossEntity {

  def name: String = SpiritOfNara.name

  def shape: Circle = SpiritOfNara.shape

  def abilityNames: Map[AbilityId, String] = SpiritOfNara.abilityNames

  def changeTarget(newTargetId: Entity.Id): SpiritOfNara = copy(targetId = newTargetId)

  def changeDamageThreats(threatId: Entity.Id, delta: ThreatAmount): SpiritOfNara =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  def changeHealingThreats(threatId: Entity.Id, delta: ThreatAmount): SpiritOfNara =
    copy(healingThreats = healingThreats.addAmount(threatId, delta))

  protected def patchLifeTotal(newLife: Double): SpiritOfNara = copy(life = newLife)

  def abilities: Set[AbilityId] = abilityNames.keySet

  def useAbility(ability: Ability): SpiritOfNara = copy(
    relevantUsedAbilities = relevantUsedAbilities + (ability.abilityId -> ability),
    resourceAmount = resourceAmount - ability.cost
  )

  def maxResourceAmount: Double = SpiritOfNara.maxMana

  protected def patchResourceAmount(newResourceAmount: ResourceAmount): SpiritOfNara =
    copy(resourceAmount = newResourceAmount)

  def move(
      time: Long,
      position: Complex,
      direction: Angle,
      rotation: Angle,
      speed: Double,
      moving: Boolean
  ): SpiritOfNara =
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
        SpiritOfNara.autoAttackDamage,
        SpiritOfNara.autoAttackTickRate,
        Mana,
        SpiritOfNara.meleeRange
      )
    ).filter(_.canBeCastBoolean(gameState, time)).filter(canUseAbilityBoolean(_, time, gameState))

  /** When the ability was last used (or [[Long.MinValue]] if never). */
  def lastUse(abilityId: AbilityId): Long =
    relevantUsedAbilities.get(abilityId).fold(Long.MinValue)(_.time)

  /** When the last of the special abilities was used. */
  def lastSpecial: Long = SpiritOfNara.specialAbilities.map(lastUse).max

  /** The Magnetic Pull was used, and its Arcane Explosion has not been cast yet. */
  def arcaneExplosionPending: Boolean =
    lastUse(Ability.spiritOfNaraMagneticPull) > lastUse(Ability.spiritOfNaraArcaneExplosion)

  /** It drank since the last Pyroblast. */
  def pyroblastPending(gameState: GameState): Boolean =
    lastUse(Ability.spiritOfNaraMassPolymorph) > lastUse(Ability.spiritOfNaraPyroblast) &&
      !isDrinking(gameState)

  def isDrinking(gameState: GameState): Boolean = gameState.hasBuffOfType[Drinking](id)

  /** The wave of Water Elementals it should summon now, if any. */
  def dueElementalWave: Option[Int] = SummonWaterElementals.dueWave(
    relevantUsedAbilities.get(Ability.spiritOfNaraSummonElementals).fold(0) {
      case summon: SummonWaterElementals => summon.wave
      case _ => 0
    },
    life / maxLife
  )

  def needsToDrink: Boolean =
    resourceAmount.amount < SpiritOfNara.maxMana * MassPolymorph.manaThreshold

}

object SpiritOfNara extends BossFactory[SpiritOfNara] with BossMetadata {

  val name: String = "Spirit of Nara"

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

  inline def autoAttackDamage: Double = 6.6
  inline def autoAttackTickRate: Long = 1500L

  inline def maxLife: Double = 40000

  inline def maxMana: Double = 1500

  /** Time between two special abilities (Arcane Explosion, Flame Wreath and Blizzard). */
  inline def specialInterval: Long = 30000L

  val specialAbilities: Vector[AbilityId] = Vector(
    Ability.spiritOfNaraMagneticPull,
    Ability.spiritOfNaraFlameWreath,
    Ability.spiritOfNaraBlizzard
  )

  val abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId                 -> "Auto attack",
    Ability.spiritOfNaraFireball         -> Fireball.name,
    Ability.spiritOfNaraFrostbolt        -> Frostbolt.name,
    Ability.spiritOfNaraArcaneMissiles   -> ArcaneMissiles.name,
    Ability.spiritOfNaraCounterspell     -> Counterspell.name,
    Ability.spiritOfNaraChainsOfIce      -> ChainsOfIce.name,
    Ability.spiritOfNaraMagneticPull     -> MagneticPull.name,
    Ability.spiritOfNaraArcaneExplosion  -> ArcaneExplosion.name,
    Ability.spiritOfNaraFlameWreath      -> FlameWreath.name,
    Ability.spiritOfNaraBlizzard         -> Blizzard.name,
    Ability.spiritOfNaraMassPolymorph    -> MassPolymorph.name,
    Ability.spiritOfNaraPyroblast        -> Pyroblast.name,
    Ability.spiritOfNaraSummonElementals -> SummonWaterElementals.name,
    Ability.spiritOfNaraShadowPyroblast  -> ShadowPyroblast.name
  )

  /** The boss starts with a full mana pool. */
  private given Pointed[ResourceAmount] = Pointed.factory(ResourceAmount(maxMana, Mana))

  def initialBoss(entityId: Entity.Id, time: Long): SpiritOfNara = Pointed[SpiritOfNara].unit.copy(
    id = entityId,
    time = time,
    maxLife = maxLife,
    life = maxLife,
    relevantUsedAbilities = Map(
      Ability.spiritOfNaraFireball -> Pointed[Fireball].unit.copy(
        time = Fireball.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraFrostbolt -> Pointed[Frostbolt].unit.copy(
        time = Frostbolt.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraArcaneMissiles -> Pointed[ArcaneMissiles].unit.copy(
        time = ArcaneMissiles.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraCounterspell -> Pointed[Counterspell].unit.copy(
        time = Counterspell.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraChainsOfIce -> Pointed[ChainsOfIce].unit.copy(
        time = ChainsOfIce.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraMagneticPull -> Pointed[MagneticPull].unit.copy(
        time = MagneticPull.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraFlameWreath -> Pointed[FlameWreath].unit.copy(
        time = FlameWreath.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraBlizzard -> Pointed[Blizzard].unit.copy(
        time = Blizzard.setBeginningOfGameAbilityUse(time)
      ),
      // more recent than the pull: no explosion pending
      Ability.spiritOfNaraArcaneExplosion -> Pointed[ArcaneExplosion].unit.copy(
        time = ArcaneExplosion.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraMassPolymorph -> Pointed[MassPolymorph].unit.copy(
        time = MassPolymorph.setBeginningOfGameAbilityUse(time)
      ),
      // more recent than the polymorph: no pyroblast pending
      Ability.spiritOfNaraPyroblast -> Pointed[Pyroblast].unit.copy(
        time = Pyroblast.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraSummonElementals -> Pointed[SummonWaterElementals].unit.copy(
        time = SummonWaterElementals.setBeginningOfGameAbilityUse(time)
      ),
      Ability.spiritOfNaraShadowPyroblast -> Pointed[ShadowPyroblast].unit.copy(
        time = ShadowPyroblast.setBeginningOfGameAbilityUse(time)
      )
    )
  )

  def initialBossActions(
      entityId: Entity.Id,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    healAndDamageAwareActions(entityId, time)

  /** Removes what could still hurt the players after the victory: the rings, the storm, the
    * elementals.
    */
  def whenBossDiesActions(
      gameState: GameState,
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = {
    val entities = (gameState.allTEntities[FlameWreathRing].keys ++
      gameState.allTEntities[BlizzardCloud].keys ++
      gameState.allTEntities[WaterElemental].keys).toVector.sortBy(_.value)
    entities.map(entityId => RemoveEntity(genActionId(), time, entityId)) ++
      gameState.allBuffs.collect { case buff: (FlameWreathWatch | BlizzardTicks) =>
        RemoveBuff(genActionId(), time, buff.bearerId, buff.buffId)
      }
  }

  /** Players come in from the left of the library. The boss waits at its centre. */
  def playersStartingPosition: Complex = Complex(-320, 0)

  // ---------------------------------------------------------------------------------------------
  // The room
  // ---------------------------------------------------------------------------------------------

  /** Distance from the centre of the library to the corners of its walls. */
  inline def roomRadius: Double = 420.0

  inline def wallThickness: Double = 15.0

  inline def numberOfWalls: Int = 24

  /** Corners of the library: a polygon with many sides, close to a circle. */
  val roomVertices: Vector[Complex] = Shape.regularPolygon(numberOfWalls, roomRadius).vertices

  def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] = {
    // walls are slightly extended on both sides so that they overlap at the corners
    val extension = wallThickness / roomRadius
    roomVertices.zip(roomVertices.tail :+ roomVertices.head).map { (z1, z2) =>
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
  }

  def description: String =
    "Second boss of the first raid. The Spirit of Nara never leaves the centre of its library, " ++
      "and it does not need to: it bombards players with fire, frost and arcane spells (cut " ++
      "them!), and every 30 seconds unleashes an Arcane Explosion (run away from it), a Flame " ++
      "Wreath (stand still) or a Blizzard (stay out of the storm). Out of mana, it polymorphs " ++
      "everyone to drink, and comes back with a Pyroblast. Under 40% of its life, it calls " ++
      "Water Elementals to its rescue, and again under 20%."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(autoAttackDamage)} damage to its target.",
      autoAttackTickRate,
      0L
    ),
    AbilityDoc.fromMetadata(
      Fireball,
      s"Hurls a ball of fire at a random player, for ${formatNumber(Fireball.damage)} damage. " ++
        "Can be cut: the boss is then unable to cast fire spells for " ++
        s"${formatSeconds(SchoolLockout.duration)}."
    ),
    AbilityDoc.fromMetadata(
      Frostbolt,
      s"Hits a random player for ${formatNumber(Frostbolt.damage)} damage, and slows them by " ++
        s"${formatNumber((1 - Chilled.speedMultiplier) * 100)}% for " ++
        s"${formatSeconds(Chilled.duration)}. Can be cut: the boss is then unable to cast frost " ++
        s"spells for ${formatSeconds(SchoolLockout.duration)}."
    ),
    AbilityDoc.fromMetadata(
      ArcaneMissiles,
      s"Fires ${ArcaneMissiles.missiles} missiles at a random player, for " ++
        s"${formatNumber(ArcaneMissiles.damagePerMissile)} damage each. Can be cut: the boss is " ++
        s"then unable to cast arcane spells for ${formatSeconds(SchoolLockout.duration)}."
    ),
    AbilityDoc.fromMetadata(
      Counterspell,
      s"Interrupts every player within ${formatNumber(Counterspell.radius)} units of the boss, " ++
        s"who can't use abilities costing mana for ${formatSeconds(Counterspelled.duration)}."
    ),
    AbilityDoc.fromMetadata(
      ChainsOfIce,
      s"Roots a random player for ${formatSeconds(Chained.duration)}. Can be dispelled."
    ),
    AbilityDoc.fromMetadata(
      MagneticPull,
      "Special ability. Pulls every player right next to the boss, and slows them by " ++
        s"${formatNumber((1 - MassSlow.speedMultiplier) * 100)}% for " ++
        s"${formatSeconds(MassSlow.duration)}. The boss then casts its Arcane Explosion."
    ),
    AbilityDoc.fromMetadata(
      ArcaneExplosion,
      s"Deals ${formatNumber(ArcaneExplosion.damage)} damage to every player within " ++
        s"${formatNumber(ArcaneExplosion.radius)} units of the boss. Run!"
    ),
    AbilityDoc.fromMetadata(
      FlameWreath,
      s"Special ability. Surrounds ${FlameWreath.targets} random players with a ring of fire, " ++
        s"for ${formatSeconds(FlameWreathWatch.duration)}. If anyone crosses a ring (whether " ++
        "going in or out), they all explode, dealing " ++
        s"${formatNumber(FlameWreathWatch.explosionDamage)} damage to every player. Don't move!"
    ),
    AbilityDoc.fromMetadata(
      Blizzard,
      "Special ability. A storm goes around the room for " ++
        s"${formatSeconds(BlizzardTicks.duration)}, dealing " ++
        s"${formatNumber(BlizzardTicks.damage)} damage every second to the players under it, " ++
        "and slowing them down."
    ),
    AbilityDoc.fromMetadata(
      MassPolymorph,
      s"When its mana falls under ${formatNumber(MassPolymorph.manaThreshold * 100)}%, the boss " ++
        s"polymorphs every player for ${formatSeconds(Polymorphed.duration)} and drinks for " ++
        s"${formatSeconds(Drinking.duration)}, refilling its mana. Polymorphed players can't do " ++
        "anything, but are back to full life when they wake up."
    ),
    AbilityDoc.fromMetadata(
      Pyroblast,
      s"Right after drinking, deals ${formatNumber(Pyroblast.damage)} damage to every player."
    ),
    AbilityDoc.fromMetadata(
      SummonWaterElementals,
      s"Under $formatElementalThresholds of its life, the boss summons " ++
        s"${SummonWaterElementals.count} Water Elementals along the walls. They throw " ++
        s"Waterbolts at random players, and vanish after ${formatSeconds(Ephemeral.duration)}."
    ),
    AbilityDoc
      .fromMetadata(
        Waterbolt,
        s"Hits a random player for ${formatNumber(Waterbolt.damage)} damage. Can be cut. The " ++
          s"Water Elementals ($formatElementalLife life) can also be stunned."
      )
      .castBy("Water Elemental"),
    AbilityDoc.fromMetadata(
      ShadowPyroblast,
      s"After ${formatSeconds(ShadowPyroblast.timeToFirstAbility)}, the boss goes berserk and " ++
        s"deals ${formatNumber(ShadowPyroblast.damage)} damage to every player every " ++
        s"${formatSeconds(ShadowPyroblast.cooldown)}."
    )
  )

  private def formatElementalLife: String = formatNumber(WaterElemental.maxLife)

  /** "40%, then under 20%" */
  private def formatElementalThresholds: String =
    SummonWaterElementals.lifeThresholds
      .map(t => s"${formatNumber(t * 100)}%")
      .mkString(", then under ")
}
