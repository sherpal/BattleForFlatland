package gamelogic.abilities

import gamelogic.abilities.Ability.AbilityColour
import gamelogic.buffs.Buff
import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.gameactions.{EntityStartsCasting, PutSimpleBuff}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer, OpaqueIntCompanion, OpaqueLongCompanion}
import io.circe.{Decoder, Encoder, Json}
import services.i18n.{DummyI18n, I18n}
import utils.misc.RGBColour

import scala.reflect.ClassTag

/** A [[gamelogic.abilities.Ability]] represents an action that an entity can take besides moving.
  *
  * Each "class" has its own set of abilities that they can use. The abilities of a given class are
  * implemented in a dedicated package that has the name of the class. Abilities in two different
  * classes can be very similar, basically only changing the constants of the effect.
  *
  * Each ability has a unique id that is given in the code itself.
  *
  * Note: if an ability is though to have no cooldown nor casting time, it is best to set the
  * cooldown to a minimal amount, in order to have a GCD. perhaps in the future, this should be
  * handled automatically.
  */
trait Ability extends IdsProducer {

  /** Unique id given manually to each ability. */
  def abilityId: Ability.AbilityId

  /** Id created for each use. */
  def useId: Ability.UseId

  /** Duration (in millis) before this ability */
  def cooldown: Long

  /** Duration (in millis) the caster needs to stay still before the ability is cast Note: can be 0
    * for instant casting time, in which case it can be activated while moving.
    */
  def castingTime: Long

  /** Id of the entity that cast the spell.
    */
  def casterId: Entity.Id

  /** Game Time (in millis) at which the ability's casting is complete. */
  def time: Long

  /** Cost of the ability. */
  def cost: ResourceAmount

  /** Type of resource needed to use the ability. */
  final def resource: Resource = cost.resourceType

  /** Whether a Triangle can use its cut ability to interrupt casting */
  def canBeCut: Boolean = false

  /** What happens to the caster (besides being interrupted) when a Triangle cuts this ability. By
    * default, it is silenced.
    */
  def cutPenalty(time: Long, cutterId: Entity.Id)(using IdGeneratorContainer): Vector[GameAction] =
    Vector(PutSimpleBuff(genActionId(), time, genBuffId(), casterId, cutterId, time, Buff.silence))

  /** Generates all actions that this ability generates when completed. These actions may depend on
    * the [[gamelogic.gamestate.GameState]] at the time the ability is completed.
    */
  def createActions(gameState: GameState)(using IdGeneratorContainer): Vector[GameAction]

  /** Change the time and id of this ability, without changing the rest. */
  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability

  inline def abilityColour: AbilityColour = Ability.abilityColour(abilityId)

  /** Returns None when the ability can indeed be cast, otherwise return Some(error message).
    *
    * @param gameState
    *   state of the game at the time the entity wants to use the ability
    * @param time
    *   time at which the entity wants to use the ability
    * @return
    *   Some error message when the ability can't be cast at that time, with that [[GameState]].
    */
  def canBeCast(gameState: GameState, time: Long)(using I18n): Option[String]

  protected def canBeCastAll(gameState: GameState, time: Long)(
      checks: (GameState, Long) => Option[String]*
  ): Option[String] =
    checks.iterator.map(_(gameState, time)).collectFirst { case Some(veto) => veto }

  /** Returns whether this ability can be cast at that time with this [[GameState]].
    */
  final def canBeCastBoolean(gameState: GameState, time: Long): Boolean =
    canBeCast(gameState, time)(using /* Does not matter which one here. */ DummyI18n).isEmpty

  final def toStartCasting(time: Long): EntityStartsCasting =
    EntityStartsCasting(GameAction.Id.dummy, time, castingTime, this)

}

object Ability {

  opaque type UseId = Long
  object UseId extends OpaqueLongCompanion[UseId]

  opaque type AbilityId = Int
  object AbilityId extends OpaqueIntCompanion[AbilityId]

  private var lastAbilityId: AbilityId   = 0
  private def nextAbilityId(): AbilityId = { lastAbilityId += 1; lastAbilityId }

  def abilityIdCount: AbilityId = lastAbilityId

  import scala.compiletime.ops.int.{-, >=}
  private type TupleFill[T, N <: Int] <: Tuple = (N, N >= 0) match {
    case (0, true) => EmptyTuple
    case (_ <: Int, true) => N *: TupleFill[T, N - 1]
  }
  private def tupleFill[N <: Int, T](n: N)(elem: => T)(using ClassTag[T]): TupleFill[T, N] =
    Tuple.fromArray(Array.fill(n)(elem)).asInstanceOf[TupleFill[T, N]]

  private def make[Names <: Tuple](using
      sizeValue: ValueOf[Tuple.Size[Names]]
  ): NamedTuple.NamedTuple[Names, TupleFill[AbilityId, Tuple.Size[Names]]] = {
    val size: Tuple.Size[Names] = sizeValue.value
    tupleFill(size)(nextAbilityId())
  }

  // Every ability in the game needs a unique id, given by the order of the definitions below
  // (`abilityColour` indexes into them). The abilities are grouped by class or boss, each group
  // being a named tuple. For the classes, the order in the tuple is also the default order in the
  // action bar. The types must be written explicitly: otherwise, the opaque `AbilityId` would be
  // seen as an `Int` outside of this object.
  val simpleBulletId: AbilityId = nextAbilityId()
  val autoAttackId: AbilityId   = nextAbilityId()
  val hexagon: (
      flashHealId: AbilityId,
      hexagonHotId: AbilityId,
      hexagonZoneId: AbilityId
  ) = make
  val square: (
      tauntId: AbilityId,
      hammerHitId: AbilityId,
      enrageId: AbilityId,
      cleaveId: AbilityId,
      braceId: AbilityId,
      shieldSlamId: AbilityId,
      guardId: AbilityId
  ) = make
  val triangle: (
      energyKickId: AbilityId,
      directHitId: AbilityId,
      upgradeDirectHitId: AbilityId,
      stunId: AbilityId,
      cutId: AbilityId
  ) = make
  val pentagon: (
      pentagonBulletId: AbilityId,
      pentagonZoneId: AbilityId,
      dispelId: AbilityId,
      fractureId: AbilityId,
      pentagramId: AbilityId
  ) = make
  val boss101: (
      bigHitId: AbilityId,
      bigDotId: AbilityId,
      smallHitId: AbilityId
  ) = make
  val boss102: (
      putDamageZonesId: AbilityId,
      spawnBossHoundId: AbilityId,
      putLivingDamageZoneId: AbilityId
  ) = make
  val boss104: (
      twinDebuffsId: AbilityId,
      spawnBigGuyId: AbilityId,
      bigGuyKickId: AbilityId,
      deathMarkTriangleAttackId: AbilityId
  ) = make
  val boss105: (
      resonanceId: AbilityId,
      skewerId: AbilityId,
      volatileMarkId: AbilityId,
      shadeSiphonId: AbilityId,
      reunionId: AbilityId
  ) = make
  val boss110: (
      spawnBigGuiesId: AbilityId,
      bigGuyBrokenArmorId: AbilityId,
      placeBombPodsId: AbilityId,
      explodeBombsId: AbilityId,
      spawnSmallGuiesId: AbilityId,
      creepingShadowTickId: AbilityId
  ) = make
  val sirOfVice: (
      cleansingNovaId: AbilityId,
      punishmentId: AbilityId,
      sacredGroundId: AbilityId,
      holyFlameId: AbilityId,
      holyWrathId: AbilityId
  ) = make
  val spiritOfNara: (
      fireballId: AbilityId,
      frostboltId: AbilityId,
      arcaneMissilesId: AbilityId,
      counterspellId: AbilityId,
      chainsOfIceId: AbilityId,
      magneticPullId: AbilityId,
      arcaneExplosionId: AbilityId,
      flameWreathId: AbilityId,
      blizzardId: AbilityId,
      massPolymorphId: AbilityId,
      pyroblastId: AbilityId,
      summonElementalsId: AbilityId,
      waterboltId: AbilityId,
      shadowPyroblastId: AbilityId
  ) = make

  opaque type AbilityColour <: RGBColour = RGBColour

  private val _abilitiesColours: Array[AbilityColour] =
    RGBColour.repeatedColours.zipWithIndex.takeWhile(_(1) < lastAbilityId).map(_(0)).toArray

  inline def abilityColour(abilityId: AbilityId): AbilityColour = _abilitiesColours(abilityId - 1)

  val abilitiesColours: Vector[AbilityColour] = _abilitiesColours.toVector

  /** Global cooldown. Not sure if this should be there... */
  inline def gcd = 200L

  /** Encoding. Replace this by more performant stuff in the future. */
  import cats.syntax.functor.*
  import io.circe.generic.auto.*
  import io.circe.syntax.*

  private def customEncode[A <: Ability](a: A, name: String)(using Encoder[A]): Json =
    a.asJson.mapObject(_.add("ability_name", Json.fromString(name)))

  given Encoder[Ability] = Encoder.instance {
    case x: boss.boss101.BigDot => customEncode(x, "boss.boss101.BigDot")
    case x: boss.boss101.BigHit => customEncode(x, "boss.boss101.BigHit")
    case x: boss.boss101.SmallHit => customEncode(x, "boss.boss101.SmallHit")
    case x: boss.boss102.PutDamageZones => customEncode(x, "boss.boss102.PutDamageZones")
    case x: boss.boss102.PutLivingDamageZoneOnTarget =>
      customEncode(x, "boss.boss102.PutLivingDamageZoneOnTarget")
    case x: boss.boss102.SpawnHound => customEncode(x, "boss.boss102.SpawnHound")
    case x: boss.boss104.SpawnBigGuy => customEncode(x, "boss.boss104.SpawnBigGuy")
    case x: boss.boss104.TwinDebuffs => customEncode(x, "boss.boss104.TwinDebuffs")
    case x: boss.boss104.BigGuyKick => customEncode(x, "boss.boss104.BigGuyKick")
    case x: gamelogic.abilities.hexagon.FlashHeal => customEncode(x, "hexagon.FlashHeal")
    case x: gamelogic.abilities.hexagon.HexagonHot => customEncode(x, "hexagon.HexagonHot")
    case x: gamelogic.abilities.pentagon.CreatePentagonBullet =>
      customEncode(x, "pentagon.CreatePentagonBullet")
    case x: gamelogic.abilities.pentagon.CreatePentagonZone =>
      customEncode(x, "pentagon.CreatePentagonZone")
    case x: gamelogic.abilities.pentagon.Fracture => customEncode(x, "pentagon.Fracture")
    case x: gamelogic.abilities.pentagon.Pentagram => customEncode(x, "pentagon.Pentagram")
    case x: gamelogic.abilities.square.Brace => customEncode(x, "square.Brace")
    case x: gamelogic.abilities.square.Cleave => customEncode(x, "square.Cleave")
    case x: gamelogic.abilities.square.Enrage => customEncode(x, "square.Enrage")
    case x: gamelogic.abilities.square.Guard => customEncode(x, "square.Guard")
    case x: gamelogic.abilities.square.HammerHit => customEncode(x, "square.HammerHit")
    case x: gamelogic.abilities.square.ShieldSlam => customEncode(x, "square.ShieldSlam")
    case x: gamelogic.abilities.square.Taunt => customEncode(x, "square.Taunt")
    case x: gamelogic.abilities.triangle.DirectHit => customEncode(x, "triangle.DirectHit")
    case x: gamelogic.abilities.triangle.UpgradeDirectHit =>
      customEncode(x, "triangle.UpgradeDirectHit")
    case x: AutoAttack => customEncode(x, "AutoAttack")
    case x: SimpleBullet => customEncode(x, "SimpleBullet")
  }

  private def customDecoder[A <: Ability](name: String)(implicit
      decoder: Decoder[A]
  ): Decoder[Ability] =
    decoder.validate(_.get[String]("ability_name").contains(name), s"Not a $name instance").widen

  given Decoder[Ability] = List[Decoder[Ability]](
    customDecoder[boss.boss101.BigDot]("boss.boss101.BigDot"),
    customDecoder[boss.boss101.BigHit]("boss.boss101.BigHit"),
    customDecoder[boss.boss101.SmallHit]("boss.boss101.SmallHit"),
    customDecoder[boss.boss102.PutLivingDamageZoneOnTarget](
      "boss.boss102.PutLivingDamageZoneOnTarget"
    ),
    customDecoder[boss.boss102.PutDamageZones]("boss.boss102.PutDamageZones"),
    customDecoder[boss.boss102.SpawnHound]("boss.boss102.SpawnHound"),
    customDecoder[boss.boss104.BigGuyKick]("boss.boss104.BigGuyKick"),
    customDecoder[boss.boss104.TwinDebuffs]("boss.boss104.TwinDebuffs"),
    customDecoder[boss.boss104.SpawnBigGuy]("boss.boss104.SpawnBigGuy"),
    customDecoder[gamelogic.abilities.hexagon.FlashHeal]("hexagon.FlashHeal"),
    customDecoder[gamelogic.abilities.hexagon.HexagonHot]("hexagon.HexagonHot"),
    customDecoder[gamelogic.abilities.pentagon.CreatePentagonBullet](
      "pentagon.CreatePentagonBullet"
    ),
    customDecoder[gamelogic.abilities.pentagon.CreatePentagonZone]("pentagon.CreatePentagonZone"),
    customDecoder[gamelogic.abilities.pentagon.Fracture]("pentagon.Fracture"),
    customDecoder[gamelogic.abilities.pentagon.Pentagram]("pentagon.Pentagram"),
    customDecoder[gamelogic.abilities.square.Brace]("square.Brace"),
    customDecoder[gamelogic.abilities.square.Cleave]("square.Cleave"),
    customDecoder[gamelogic.abilities.square.Enrage]("square.Enrage"),
    customDecoder[gamelogic.abilities.square.Guard]("square.Guard"),
    customDecoder[gamelogic.abilities.square.HammerHit]("square.HammerHit"),
    customDecoder[gamelogic.abilities.square.ShieldSlam]("square.ShieldSlam"),
    customDecoder[gamelogic.abilities.square.Taunt]("square.Taunt"),
    customDecoder[gamelogic.abilities.triangle.DirectHit]("triangle.DirectHit"),
    customDecoder[gamelogic.abilities.triangle.UpgradeDirectHit]("triangle.UpgradeDirectHit"),
    customDecoder[AutoAttack]("AutoAttack"),
    customDecoder[SimpleBullet]("SimpleBullet")
  ).reduceLeft(_ or _)

}
