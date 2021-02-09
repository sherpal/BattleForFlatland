package gamelogic.gamestate.gameactions.boss110

import gamelogic.entities.Entity
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.WithEntity
import gamelogic.entities.boss.boss110.BigGuy
import cats.kernel.Monoid

/**
  * Spawn one [[BigGuy]] for each element in the `idsAndPositions` List.
  *
  * Another way of doing could be to create one action per [[BigGuy]], but then we are creating multiple
  * actions for nothing, and mechanism like action transformers have more work.
  */
final case class AddBigGuies(id: GameAction.Id, time: Long, idsAndPositions: List[(Entity.Id, Complex)])
    extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    Monoid[GameStateTransformer].combineAll(
      idsAndPositions.map {
        case (entityId, startingPosition) =>
          new WithEntity(
            BigGuy(
              entityId,
              time,
              startingPosition,
              0,
              0,
              BigGuy.fullSpeed,
              moving = false,
              BigGuy.maxLife,
              Map.empty,
              entityId,
              Map.empty
            ),
            time
          )
      }
    )

  def isLegal(gameState: GameState): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

  /**
    * Generates the list of [[AddBigGuies.AddBigGuy]] corresponding to the list of this
    * action. This is used by the AI manager to have one "action" per [[BigGuy]].
    */
  def splitToSingleAddBigGuies: List[AddBigGuies.AddBigGuy] =
    idsAndPositions.map(_._1).map(AddBigGuies.AddBigGuy)

}

object AddBigGuies {
  final case class AddBigGuy(entityId: Entity.Id) extends GameAction.EntityCreatorAction
}
