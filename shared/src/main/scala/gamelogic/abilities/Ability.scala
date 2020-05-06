package gamelogic.abilities

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.utils.EntityIdGenerator
import io.circe.{Decoder, Encoder, Json}

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

  /**
    * Generates all actions that this ability generates when completed.
    *These actions may depend on the [[gamelogic.gamestate.GameState]] at the time the ability is completed.
    */
  def createActions(gameState: GameState, entityIdGenerator: EntityIdGenerator): List[GameAction]

  /** Change the time and id of this ability, without changing the rest. */
  def copyWithNewTimeAndId(newTime: Long, newId: Ability.UseId): Ability

}

object Ability {

  type UseId = Long

  type AbilityId = Int

  final val simpleBulletId: AbilityId = 1

  /** Encoding. Replace this by more performant stuff in the future. */
  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private def customEncode[A <: Ability](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("ability_name", Json.fromString(name)))

  implicit val encoder: Encoder[Ability] = Encoder.instance {
    case x: SimpleBullet => customEncode(x, "SimpleBullet")
  }

  private def customDecoder[A <: Ability](name: String)(implicit decoder: Decoder[A]): Decoder[Ability] =
    decoder.validate(_.get[String]("ability_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[Ability] = List[Decoder[Ability]](
    customDecoder[SimpleBullet]("SimpleBullet")
  ).reduceLeft(_ or _)

}
