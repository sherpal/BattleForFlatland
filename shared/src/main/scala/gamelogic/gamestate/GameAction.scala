package gamelogic.gamestate

import gamelogic.gamestate.gameactions._
import gamelogic.gamestate.statetransformers.GameStateTransformer
import io.circe.{Decoder, Encoder, Json}

trait GameAction extends Ordered[GameAction] {

  val id: GameAction.Id

  /** Time at which the action occurred (in millis) */
  val time: Long

  /**
    * Describes how this action affects a given GameState.
    *
    * This is done by first applying all transformation from [[gamelogic.buffs.PassiveBuff]] to this action, then
    * folding over all created actions.
    *
    * We can't use monoid aggregation here otherwise all action transformers are created with the first game state
    * and not the folded ones.
    */
  final def apply(gameState: GameState): GameState =
    gameState.applyActionChangers(this).foldLeft(gameState) { (currentGameState, nextAction) =>
      nextAction.createAndApplyGameStateTransformer(currentGameState)
    }

  /**
    * Creates the [[gamelogic.gamestate.statetransformers.GameStateTransformer]] that will effectively affect the game.
    * If more than one building block must be used, you can compose them using their `++` method.
    */
  def createGameStateTransformer(gameState: GameState): GameStateTransformer

  def createAndApplyGameStateTransformer(gameState: GameState): GameState =
    createGameStateTransformer(gameState)(gameState)

  /**
    * Returns whether this action is legal at that particular point in time, i.e., for that
    * [[gamelogic.gamestate.GameState]].
    */
  def isLegal(gameState: GameState): Boolean

  /** We compare ids if the time are the same so that there never is ambiguity. */
  final def compare(that: GameAction): Int = this.time compare that.time match {
    case 0 => this.id compare that.id
    case x => x
  }

  def changeId(newId: GameAction.Id): GameAction

}

object GameAction {

  type Id = Long

  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private def customEncode[A <: GameAction](a: A, name: String)(implicit encoder: Encoder[A]): Json =
    a.asJson.mapObject(_.add("action_name", Json.fromString(name)))

  implicit val encoder: Encoder[GameAction] = Encoder.instance {
    case x: AddDummyMob              => customEncode(x, "AddDummyMob")
    case x: AddPlayer                => customEncode(x, "AddPlayer")
    case x: AddPlayerByClass         => customEncode(x, "AddPlayerByClass")
    case x: ChangeTarget             => customEncode(x, "ChangeTarget")
    case x: DummyEntityMoves         => customEncode(x, "DummyEntityMoves")
    case x: EndGame                  => customEncode(x, "EndGame")
    case x: EntityCastingInterrupted => customEncode(x, "EntityCastingInterrupted")
    case x: EntityGetsHealed         => customEncode(x, "EntityGetsHealed")
    case x: EntityResourceChanges    => customEncode(x, "EntityResourceChanges")
    case x: EntityStartsCasting      => customEncode(x, "EntityStartsCasting")
    case x: EntityTakesDamage        => customEncode(x, "EntityTakesDamage")
    case x: GameStart                => customEncode(x, "GameStart")
    case x: MovingBodyMoves          => customEncode(x, "MovingBodyMoves")
    case x: NewSimpleBullet          => customEncode(x, "NewSimpleBullet")
    case x: PutSimpleBuff            => customEncode(x, "PutSimpleBuff")
    case x: PutConstantDot           => customEncode(x, "PutConstantDot")
    case x: RemoveBuff               => customEncode(x, "RemoveBuff")
    case x: RemoveEntity             => customEncode(x, "RemoveEntity")
    case x: SpawnBoss                => customEncode(x, "SpawnBoss")
    case x: ThreatToEntityChange     => customEncode(x, "ThreatToEntityChange")
    case x: TickerBuffTicks          => customEncode(x, "TickerBuffTicks")
    case x: UpdateConstantHot        => customEncode(x, "UpdateConstantHot")
    case x: UpdateTimestamp          => customEncode(x, "UpdateTimestamp")
    case x: UseAbility               => customEncode(x, "UseAbility")
  }

  private def customDecoder[A <: GameAction](name: String)(implicit decoder: Decoder[A]): Decoder[GameAction] =
    decoder.validate(_.get[String]("action_name").contains(name), s"Not a $name instance").widen

  implicit val decoder: Decoder[GameAction] = List[Decoder[GameAction]](
    customDecoder[AddDummyMob]("AddDummyMob"),
    customDecoder[AddPlayer]("AddPlayer"),
    customDecoder[AddPlayerByClass]("AddPlayerByClass"),
    customDecoder[ChangeTarget]("ChangeTarget"),
    customDecoder[DummyEntityMoves]("DummyEntityMoves"),
    customDecoder[EndGame]("EndGame"),
    customDecoder[EntityCastingInterrupted]("EntityCastingInterrupted"),
    customDecoder[EntityGetsHealed]("EntityGetsHealed"),
    customDecoder[EntityResourceChanges]("EntityResourceChanges"),
    customDecoder[EntityStartsCasting]("EntityStartsCasting"),
    customDecoder[EntityTakesDamage]("EntityTakesDamage"),
    customDecoder[GameStart]("GameStart"),
    customDecoder[MovingBodyMoves]("MovingBodyMoves"),
    customDecoder[NewSimpleBullet]("NewSimpleBullet"),
    customDecoder[PutSimpleBuff]("PutSimpleBuff"),
    customDecoder[PutConstantDot]("PutConstantDot"),
    customDecoder[RemoveBuff]("RemoveBuff"),
    customDecoder[RemoveEntity]("RemoveEntity"),
    customDecoder[SpawnBoss]("SpawnBoss"),
    customDecoder[ThreatToEntityChange]("ThreatToEntityChange"),
    customDecoder[TickerBuffTicks]("TickerBuffTicks"),
    customDecoder[UpdateConstantHot]("UpdateConstantHot"),
    customDecoder[UpdateTimestamp]("UpdateTimestamp"),
    customDecoder[UseAbility]("UseAbility")
  ).reduceLeft(_ or _)

}
