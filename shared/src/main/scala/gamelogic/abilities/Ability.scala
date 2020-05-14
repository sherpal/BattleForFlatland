package gamelogic.abilities

import gamelogic.entities.Resource.ResourceAmount
import gamelogic.entities.{Entity, Resource}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.{BuffIdGenerator, EntityIdGenerator}
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
  val abilityId: Ability.AbilityId

  /** Id created for each use. */
  val useId: Ability.UseId

  /** Duration (in millis) before this ability  */
  val cooldown: Long

  /**
    * Duration (in millis) the caster needs to stay still before the ability is cast
    * Note: can be 0 for instant casting time, in which case it can be activated while moving.
    */
  val castingTime: Long

  /**
    * Id of the entity that cast the spell.
    */
  val casterId: Entity.Id

  /** Game Time (in millis) at which the ability's casting is complete.  */
  val time: Long

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
  )(implicit entityIdGenerator: EntityIdGenerator, buffIdGenerator: BuffIdGenerator): List[GameAction]

  /** Change the time and id of this ability, without changing the rest. */
  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability

  def isInRange(gameState: GameState, time: Long): Boolean = true

}

object Ability {

  type UseId = Long

  type AbilityId = Int

  final val simpleBulletId: AbilityId      = 1
  final val hexagonFlashHealId: AbilityId  = 2
  final val hexagonHexagonHotId: AbilityId = 3
  final val squareTauntId: AbilityId       = 4

  /** Encoding. Replace this by more performant stuff in the future. */
  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private def customEncode[A <: Ability](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("ability_name", Json.fromString(name)))

  implicit val encoder: Encoder[Ability] = Encoder.instance {
    case x: hexagon.FlashHeal  => customEncode(x, "hexagon.FlashHeal")
    case x: hexagon.HexagonHot => customEncode(x, "hexagon.HexagonHot")
    case x: SimpleBullet       => customEncode(x, "SimpleBullet")
  }

  private def customDecoder[A <: Ability](name: String)(implicit decoder: Decoder[A]): Decoder[Ability] =
    decoder.validate(_.get[String]("ability_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[Ability] = List[Decoder[Ability]](
    customDecoder[hexagon.FlashHeal]("hexagon.FlashHeal"),
    customDecoder[hexagon.HexagonHot]("hexagon.HexagonHot"),
    customDecoder[SimpleBullet]("SimpleBullet")
  ).reduceLeft(_ or _)

}
