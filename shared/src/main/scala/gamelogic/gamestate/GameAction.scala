package gamelogic.gamestate

import gamelogic.gamestate.gameactions.{AddPlayer, DummyEntityMoves, EndGame, GameStart, UpdateTimestamp}
import io.circe.{Decoder, Encoder, Json}
import io.circe.Json.JString

trait GameAction extends Ordered[GameAction] {

  val id: GameAction.Id

  /** Time at which the action occurred (in millis) */
  val time: Long

  /** Describes how this action affects a given GameState. */
  def apply(gameState: GameState): GameState

  def isLegal(gameState: GameState): Boolean

  final def compare(that: GameAction): Int = this.time compare that.time

}

object GameAction {

  type Id = Long

  import io.circe.generic.auto._
  import io.circe.syntax._
  import cats.syntax.functor._

  private def customEncode[A <: GameAction](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("action_name", Json.fromString(name)))

  implicit val encoder: Encoder[GameAction] = Encoder.instance {
    case x: AddPlayer        => customEncode(x, "AddPlayer")
    case x: DummyEntityMoves => customEncode(x, "DummyEntityMoves")
    case x: EndGame          => customEncode(x, "EndGame")
    case x: GameStart        => customEncode(x, "GameStart")
    case x: UpdateTimestamp  => customEncode(x, "UpdateTimestamp")
  }

  private def customDecoder[A <: GameAction](name: String)(implicit decoder: Decoder[A]): Decoder[GameAction] =
    decoder.validate(_.get[String]("action_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[GameAction] = List[Decoder[GameAction]](
    customDecoder[AddPlayer]("AddPlayer"),
    customDecoder[DummyEntityMoves]("DummyEntityMoves"),
    customDecoder[EndGame]("EndGame"),
    customDecoder[GameStart]("GameStart"),
    customDecoder[UpdateTimestamp]("UpdateTimestamp")
  ).reduceLeft(_ or _)

}
