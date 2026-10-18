package gamelogic.entities.boss.dawnoftime

import gamelogic.abilities.Ability.AbilityId
import gamelogic.abilities.WithTargetAbility.Distance
import gamelogic.abilities.boss.boss104.{SpawnBigGuy, TwinDebuffs}
import gamelogic.abilities.{Ability, AutoAttack}
import gamelogic.abilities.boss.boss104.{BigGuyKick, DeathMarkTriangleAttack}
import gamelogic.buffs.boss.boss104.TwinDebuff
import gamelogic.docs.{formatNumber, formatPeriod, AbilityDoc, BossMetadata}
import gamelogic.entities.boss.boss104.{BigGuy, DeathMarkTriangle}
import gamelogic.entities.Entity.{Id, TeamId}
import gamelogic.entities.Resource.{NoResource, ResourceAmount}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.*
import gamelogic.entities.WithThreat.ThreatAmount
import gamelogic.entities.boss.{Boss101, BossEntity, BossFactory}
import gamelogic.entities.staticstuff.Obstacle
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.CreateObstacle
import gamelogic.physics.Complex
import gamelogic.physics.shape.Circle
import gamelogic.utils.IdGeneratorContainer
import models.bff.outofgame.PlayerClasses
import models.syntax.Pointed

/** Boss104 is the reference implementation for new bosses: see `make-a-boss.md` at the root of the
  * repository, which walks through every file of this encounter.
  *
  * Its three mechanics are
  *   - the [[gamelogic.abilities.boss.boss104.TwinDebuffs]]: two players are cursed and two
  *     matching coloured circles appear on the ground. The Pentagons must dispel each cursed player
  *     while standing in the circle of the matching colour.
  *   - the [[gamelogic.abilities.boss.boss104.SpawnBigGuy]]: adds that the Triangle has to tank,
  *     place, and interrupt.
  *   - the marks left where the big guys die.
  *
  * Note that this is a `case class` on purpose: the `copy` method is what all the
  * [[gamelogic.entities.MovingBody]]/[[gamelogic.entities.WithThreat]] patch methods below are
  * implemented with, and `Pointed[Boss104].unit` (used in `initialBoss`) needs it as well.
  */
final case class Boss104(
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

  override def shape: Circle = Boss101.shape

  override def changeDamageThreats(threatId: Id, delta: ThreatAmount): WithThreat =
    copy(damageThreats = damageThreats.addAmount(threatId, delta))

  override protected def patchResourceAmount(newResourceAmount: ResourceAmount): WithAbilities =
    this

  override def abilities: Set[AbilityId] = Set(
    Ability.autoAttackId,
    Ability.boss104TwinDebuffs,
    Ability.boss104SpawnBigGuy,
    Ability.boss104DeathMarkTriangleAttack
  )

  override def abilityNames: Map[AbilityId, String] = Map(
    Ability.autoAttackId                   -> "Auto Attack",
    Ability.boss104TwinDebuffs             -> "Twin Debuffs",
    Ability.boss104SpawnBigGuy             -> "Big Guy",
    Ability.boss104DeathMarkTriangleAttack -> "Death Mark Triangle"
  )

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

  override def name: String = Boss104.name

  /** Candidate auto attack for this instant, if the boss is allowed to use it right now.
    *
    * The two filters are complementary: `canBeCast` checks the ability's own preconditions (here,
    * being in melee range of the target) while `canUseAbilityBoolean` checks the entity's ones
    * (owning the ability, cooldown, resource, silence). The [[application.ai.boss]] controller only
    * has to decide *whether it wants to*, not *whether it may*.
    */
  def maybeAutoAttack(time: Long, gameState: GameState): Option[AutoAttack] =
    Some(
      AutoAttack(
        Ability.UseId.zero,
        time,
        id,
        targetId,
        Boss104.autoAttackDamage,
        Boss104.autoAttackTickRate,
        NoResource,
        Boss104.meleeRange
      )
    ).filter(_.canBeCast(gameState, time).isEmpty).filter(canUseAbilityBoolean(_, time, gameState))

}

object Boss104 extends BossFactory[Boss104] with BossMetadata {

  override def intendedFor: Int = 5

  override def maybeAIComposition: Option[List[PlayerClasses]] = Some(
    List(
      PlayerClasses.Square,
      PlayerClasses.Pentagon,
      PlayerClasses.Pentagon,
      PlayerClasses.Triangle,
      PlayerClasses.Hexagon
    )
  )

  inline def shape: Circle = Boss101.shape

  inline def meleeRange: Distance = shape.radius + 20.0
  inline def rangeRange: Distance = 2000.0 // basically infinite distance

  inline def autoAttackDamage: Double = 5.0
  inline def autoAttackTickRate: Long = 1000L

  inline def maxLife: Double = 22000

  /** The boss as it is at the very beginning of the game.
    *
    * `relevantUsedAbilities` is pre-filled with *fake past usages* of each ability. Since cooldowns
    * are computed as `now - ability.time <= ability.cooldown`, seeding the time at `time - cooldown
    * + timeToFirstUse` makes the ability come off cooldown exactly `timeToFirstUse` milliseconds
    * after the pull. This is how we give players a warm-up before the first big mechanic.
    */
  override def initialBoss(entityId: Id, time: Long): Boss104 = Pointed[Boss104].unit.copy(
    id = entityId,
    time = time,
    maxLife = maxLife,
    life = maxLife,
    speed = Boss102.fullSpeed,
    relevantUsedAbilities = Map(
      Ability.boss104TwinDebuffs -> Pointed[TwinDebuffs].unit.copy(
        time = time - TwinDebuffs.cooldown + TwinDebuffs.timeToFirstUse
      ),
      Ability.boss104SpawnBigGuy -> Pointed[SpawnBigGuy].unit.copy(
        time = time - SpawnBigGuy.cooldown + SpawnBigGuy.timeToFirstUse
      )
    )
  )

  override def initialBossActions(entityId: Id, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = healAndDamageAwareActions(entityId, time)

  override def whenBossDiesActions(gameState: GameState, time: Long)(using
      IdGeneratorContainer
  ): Vector[GameAction] = Vector.empty

  val size = 400.0

  def gameBoundariesActions(
      time: Long
  )(using IdGeneratorContainer): Vector[CreateObstacle] = Obstacle.squareGameArea(time, size)

  /** Actions applied at the origin of time, before anyone is spawned: this is where the topology of
    * the room is decided. Boss104 simply fights in a big empty square.
    */
  def stagingBossActions(
      time: Long
  )(using IdGeneratorContainer): Vector[GameAction] =
    gameBoundariesActions(time)

  override def playersStartingPosition: Complex = 0

  val name: String = "Boss 104"


  def description: String =
    "Two players regularly get cursed, and the Pentagons must dispel them from the right spot. " ++
      "Meanwhile the boss summons big guys that the Triangle must keep in check and interrupt. " ++
      "Where the big guys die matters: three of them leave a deadly triangle on the ground."

  def abilitiesDoc: Vector[AbilityDoc] = Vector(
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(autoAttackDamage)} damage to its target.",
      autoAttackTickRate,
      0L
    ),
    AbilityDoc(
      Ability.boss104TwinDebuffs,
      "Twin Debuffs",
      "Curses two random players with debuffs of two different colours, dealing " ++
        s"${formatNumber(TwinDebuff.damageOnTick)} damage every " ++
        s"${formatPeriod(TwinDebuff.tickRate)}, and drops a circle of each colour in the room. " ++
        "The curses must be dispelled by a Pentagon standing in the circle of the matching " ++
        s"colour, otherwise the Pentagon takes ${formatNumber(TwinDebuff.wrongDispelDamage)} damage.",
      TwinDebuffs.cooldown,
      TwinDebuffs.castingTime
    ),
    AbilityDoc(
      Ability.boss104SpawnBigGuy,
      "Big Guy",
      s"Summons a big guy with ${formatNumber(BigGuy.maxLife)} life points. When a big guy dies, " ++
        "it leaves a death mark on the ground. When three death marks are down, they form a " ++
        "Death Mark Triangle.",
      SpawnBigGuy.cooldown,
      SpawnBigGuy.castingTime
    ),
    AbilityDoc(
      Ability.boss104DeathMarkTriangleAttack,
      "Death Mark Triangle",
      "Only cast while a Death Mark Triangle stands in the room, twice in a row. The first time, " ++
        s"players inside the triangle take ${formatNumber(DeathMarkTriangle.deadlyDamage)} " ++
        "damage, and players outside of it " ++
        s"${formatNumber(DeathMarkTriangle.safeDamage(DeathMarkTriangle.firstStage))}. The " ++
        "second time, it's the opposite: players outside take " ++
        s"${formatNumber(DeathMarkTriangle.deadlyDamage)} damage, and players inside " ++
        s"${formatNumber(DeathMarkTriangle.safeDamage(DeathMarkTriangle.secondStage))}.",
      0L,
      DeathMarkTriangleAttack.castingTime
    ),
    AbilityDoc(
      Ability.autoAttackId,
      "Auto Attack",
      s"Melee attack dealing ${formatNumber(BigGuy.damageOnTick)} damage to its target.",
      BigGuy.tickRate,
      0L
    ).castBy("Big Guy"),
    AbilityDoc(
      Ability.boss104BigGuyKick,
      "Kick",
      s"Kicks its target for ${formatNumber(BigGuyKick.damage)} damage. Can be interrupted " ++
        "(with the Triangle's Cut, for example).",
      BigGuyKick.cooldown,
      BigGuyKick.castingTime
    ).castBy("Big Guy")
  )
}
