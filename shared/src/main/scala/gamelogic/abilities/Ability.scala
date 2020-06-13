package gamelogic.abilities

import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.IdGeneratorContainer
import io.circe.{Decoder, Encoder, Json}

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

  def canBeCast(gameState: GameState, time: Long): Boolean

}

object Ability {

  type UseId = Long

  type AbilityId = Int

  final val simpleBulletId: AbilityId           = 1
  final val hexagonFlashHealId: AbilityId       = 2
  final val hexagonHexagonHotId: AbilityId      = 3
  final val squareTauntId: AbilityId            = 4
  final val squareHammerHit: AbilityId          = 5
  final val boss101BigHitId: AbilityId          = 6
  final val boss101BigDotId: AbilityId          = 7
  final val triangleDirectHit: AbilityId        = 8
  final val triangleUpgradeDirectHit: AbilityId = 9
  final val pentagonPentagonBullet: AbilityId   = 10
  final val boss101SmallHitId: AbilityId        = 11
  final val squareEnrageId: AbilityId           = 12

  @inline final def gcd = 200L

  /** Encoding. Replace this by more performant stuff in the future. */
  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private def customEncode[A <: Ability](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("ability_name", Json.fromString(name)))

  implicit val encoder: Encoder[Ability] = Encoder.instance {
    case x: boss.boss101.BigDot           => customEncode(x, "boss.boss101.BigDot")
    case x: boss.boss101.BigHit           => customEncode(x, "boss.boss101.BigHit")
    case x: boss.boss101.SmallHit         => customEncode(x, "boss.boss101.SmallHit")
    case x: hexagon.FlashHeal             => customEncode(x, "hexagon.FlashHeal")
    case x: hexagon.HexagonHot            => customEncode(x, "hexagon.HexagonHot")
    case x: pentagon.CreatePentagonBullet => customEncode(x, "pentagon.CreatePentagonBullet")
    case x: square.Enrage                 => customEncode(x, "square.Enrage")
    case x: square.HammerHit              => customEncode(x, "square.HammerHit")
    case x: square.Taunt                  => customEncode(x, "square.Taunt")
    case x: triangle.DirectHit            => customEncode(x, "triangle.DirectHit")
    case x: triangle.UpgradeDirectHit     => customEncode(x, "triangle.UpgradeDirectHit")
    case x: SimpleBullet                  => customEncode(x, "SimpleBullet")
  }

  private def customDecoder[A <: Ability](name: String)(implicit decoder: Decoder[A]): Decoder[Ability] =
    decoder.validate(_.get[String]("ability_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[Ability] = List[Decoder[Ability]](
    customDecoder[boss.boss101.BigDot]("boss.boss101.BigDot"),
    customDecoder[boss.boss101.BigHit]("boss.boss101.BigHit"),
    customDecoder[boss.boss101.SmallHit]("boss.boss101.SmallHit"),
    customDecoder[hexagon.FlashHeal]("hexagon.FlashHeal"),
    customDecoder[hexagon.HexagonHot]("hexagon.HexagonHot"),
    customDecoder[pentagon.CreatePentagonBullet]("pentagon.CreatePentagonBullet"),
    customDecoder[square.Enrage]("square.Enrage"),
    customDecoder[square.HammerHit]("square.HammerHit"),
    customDecoder[square.Taunt]("square.Taunt"),
    customDecoder[triangle.DirectHit]("triangle.DirectHit"),
    customDecoder[triangle.UpgradeDirectHit]("triangle.UpgradeDirectHit"),
    customDecoder[SimpleBullet]("SimpleBullet")
  ).reduceLeft(_ or _)

}
