package gamelogic.abilities

import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import io.circe.{Decoder, Encoder, Json}
import scala.annotation.tailrec

/**
  * A [[gamelogic.abilities.Ability]] represents an action that an entity can take besides moving.
  *
  * Each "class" has its own set of abilities that they can use. The abilities of a given class are implemented in a
  * dedicated package that has the name of the class. Abilities in two different classes can be very similar, basically
  * only changing the constants of the effect.
  *
  * Each ability has a unique id that is given in the code itself.
  *
  * Note: if an ability is though to have no cooldown nor casting time, it is best to set the cooldown to a minimal
  * amount, in order to have a GCD. perhaps in the future, this should be handled automatically.
  */
trait Ability {

  /** Unique id given manually to each ability. */
  def abilityId: Ability.AbilityId

  /** Id created for each use. */
  def useId: Ability.UseId

  /** Duration (in millis) before this ability  */
  def cooldown: Long

  /**
    * Duration (in millis) the caster needs to stay still before the ability is cast
    * Note: can be 0 for instant casting time, in which case it can be activated while moving.
    */
  def castingTime: Long

  /**
    * Id of the entity that cast the spell.
    */
  def casterId: Entity.Id

  /** Game Time (in millis) at which the ability's casting is complete.  */
  def time: Long

  /** Cost of the ability. */
  def cost: ResourceAmount

  /** Type of resource needed to use the ability. */
  final def resource: Resource = cost.resourceType

  /**
    * Generates all actions that this ability generates when completed.
    * These actions may depend on the [[gamelogic.gamestate.GameState]] at the time the ability is completed.
    */
  def createActions(
      gameState: GameState
  )(implicit idGeneratorContainer: IdGeneratorContainer): List[GameAction]

  /** Change the time and id of this ability, without changing the rest. */
  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability

  /**
    * Returns None when the ability can indeed be cast, otherwise return
    * Some(error message).
    *
    * @param gameState state of the game at the time the entity wants to use the ability
    * @param time time at which the entity wants to use the ability
    * @return Some error message when the ability can't be cast at that time, with that
    *         [[GameState]].
    */
  def canBeCast(gameState: GameState, time: Long): Option[String]

  protected def canBeCastAll(gameState: GameState, time: Long)(
      checks: (GameState, Long) => Option[String]*
  ): Option[String] = {
    @tailrec
    def canBeCastAllList(remainingChecks: List[(GameState, Long) => Option[String]]): Option[String] =
      remainingChecks match {
        case Nil => None
        case head :: tail =>
          head(gameState, time) match {
            case Some(value) => Some(value)
            case None        => canBeCastAllList(tail)
          }
      }

    canBeCastAllList(checks.toList)
  }

  /**
    * Returns whether this ability can be cast at that time with this [[GameState]].
    */
  final def canBeCastBoolean(gameState: GameState, time: Long): Boolean =
    canBeCast(gameState, time).isEmpty

}

object Ability {

  type UseId = Long

  type AbilityId = Int

  private var lastAbilityId: AbilityId   = 0
  private def nextAbilityId(): AbilityId = { lastAbilityId += 1; lastAbilityId }

  def abilityIdCount: AbilityId = lastAbilityId

  val simpleBulletId: AbilityId           = nextAbilityId()
  val hexagonFlashHealId: AbilityId       = nextAbilityId()
  val hexagonHexagonHotId: AbilityId      = nextAbilityId()
  val squareTauntId: AbilityId            = nextAbilityId()
  val squareHammerHit: AbilityId          = nextAbilityId()
  val boss101BigHitId: AbilityId          = nextAbilityId()
  val boss101BigDotId: AbilityId          = nextAbilityId()
  val triangleDirectHit: AbilityId        = nextAbilityId()
  val triangleEnergyKick: AbilityId       = nextAbilityId()
  val triangleUpgradeDirectHit: AbilityId = nextAbilityId()
  val triangleStun: AbilityId             = nextAbilityId()
  val pentagonPentagonBullet: AbilityId   = nextAbilityId()
  val boss101SmallHitId: AbilityId        = nextAbilityId()
  val squareEnrageId: AbilityId           = nextAbilityId()
  val boss102PutDamageZones: AbilityId    = nextAbilityId()
  val boss102SpawnBossHound: AbilityId    = nextAbilityId()
  val autoAttackId: AbilityId             = nextAbilityId()
  val squareCleaveId: AbilityId           = nextAbilityId()
  val createPentagonZoneId: AbilityId     = nextAbilityId()
  val putLivingDamageZoneId: AbilityId    = nextAbilityId()
  val boss103CleansingNovaId: AbilityId   = nextAbilityId()
  val boss103PunishmentId: AbilityId      = nextAbilityId()
  val boss103SacredGroundId: AbilityId    = nextAbilityId()
  val boss103HolyFlameId: AbilityId       = nextAbilityId()
  val pentagonDispelId: AbilityId         = nextAbilityId()
  val boss110SpawnBigGuies: AbilityId     = nextAbilityId()
  val boss110BigGuyBrokenArmor: AbilityId = nextAbilityId()
  val boss110PlaceBombPods: AbilityId     = nextAbilityId()
  val boss110ExplodeBombs: AbilityId      = nextAbilityId()
  val boss110SpawnSmallGuies: AbilityId   = nextAbilityId()

  /** Global cooldown. Not sure if this should be there... */
  @inline def gcd = 200L

  /** Encoding. Replace this by more performant stuff in the future. */
  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private def customEncode[A <: Ability](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("ability_name", Json.fromString(name)))

  implicit val encoder: Encoder[Ability] = Encoder.instance {
    case x: boss.boss101.BigDot                      => customEncode(x, "boss.boss101.BigDot")
    case x: boss.boss101.BigHit                      => customEncode(x, "boss.boss101.BigHit")
    case x: boss.boss101.SmallHit                    => customEncode(x, "boss.boss101.SmallHit")
    case x: boss.boss102.PutDamageZones              => customEncode(x, "boss.boss102.PutDamageZones")
    case x: boss.boss102.PutLivingDamageZoneOnTarget => customEncode(x, "boss.boss102.PutLivingDamageZoneOnTarget")
    case x: boss.boss102.SpawnHound                  => customEncode(x, "boss.boss102.SpawnHound")
    case x: hexagon.FlashHeal                        => customEncode(x, "hexagon.FlashHeal")
    case x: hexagon.HexagonHot                       => customEncode(x, "hexagon.HexagonHot")
    case x: pentagon.CreatePentagonBullet            => customEncode(x, "pentagon.CreatePentagonBullet")
    case x: pentagon.CreatePentagonZone              => customEncode(x, "pentagon.CreatePentagonZone")
    case x: square.Cleave                            => customEncode(x, "square.Cleave")
    case x: square.Enrage                            => customEncode(x, "square.Enrage")
    case x: square.HammerHit                         => customEncode(x, "square.HammerHit")
    case x: square.Taunt                             => customEncode(x, "square.Taunt")
    case x: triangle.DirectHit                       => customEncode(x, "triangle.DirectHit")
    case x: triangle.UpgradeDirectHit                => customEncode(x, "triangle.UpgradeDirectHit")
    case x: AutoAttack                               => customEncode(x, "AutoAttack")
    case x: SimpleBullet                             => customEncode(x, "SimpleBullet")
  }

  private def customDecoder[A <: Ability](name: String)(implicit decoder: Decoder[A]): Decoder[Ability] =
    decoder.validate(_.get[String]("ability_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[Ability] = List[Decoder[Ability]](
    customDecoder[boss.boss101.BigDot]("boss.boss101.BigDot"),
    customDecoder[boss.boss101.BigHit]("boss.boss101.BigHit"),
    customDecoder[boss.boss101.SmallHit]("boss.boss101.SmallHit"),
    customDecoder[boss.boss102.PutLivingDamageZoneOnTarget]("boss.boss102.PutLivingDamageZoneOnTarget"),
    customDecoder[boss.boss102.PutDamageZones]("boss.boss102.PutDamageZones"),
    customDecoder[boss.boss102.SpawnHound]("boss.boss102.SpawnHound"),
    customDecoder[hexagon.FlashHeal]("hexagon.FlashHeal"),
    customDecoder[hexagon.HexagonHot]("hexagon.HexagonHot"),
    customDecoder[pentagon.CreatePentagonBullet]("pentagon.CreatePentagonBullet"),
    customDecoder[pentagon.CreatePentagonZone]("pentagon.CreatePentagonZone"),
    customDecoder[square.Cleave]("square.Cleave"),
    customDecoder[square.Enrage]("square.Enrage"),
    customDecoder[square.HammerHit]("square.HammerHit"),
    customDecoder[square.Taunt]("square.Taunt"),
    customDecoder[triangle.DirectHit]("triangle.DirectHit"),
    customDecoder[triangle.UpgradeDirectHit]("triangle.UpgradeDirectHit"),
    customDecoder[AutoAttack]("AutoAttack"),
    customDecoder[SimpleBullet]("SimpleBullet")
  ).reduceLeft(_ or _)

}
