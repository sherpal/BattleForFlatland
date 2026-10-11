package gamelogic.gamestate.gameactions.boss110

import cats.kernel.Monoid
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.physics.Complex

/** Spawn one [[BigGuy]] for each element in the `idsAndPositions` List.
  *
  * Another way of doing could be to create one action per [[BigGuy]], but then we are creating
  * multiple actions for nothing, and mechanism like action transformers have more work.
  */
final case class AddBigGuies(
    id: GameAction.Id,
    time: Long,
    idsAndPositions: Vector[(Entity.Id, Complex)]
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    Monoid[GameStateTransformer].combineAll(
      idsAndPositions.map { case (entityId, startingPosition) =>
        new WithEntity(
          BigGuy(
            entityId,
            time,
            startingPosition,
            Angle.zero,
            Angle.zero,
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

  /** Generates the list of [[AddBigGuies.AddBigGuy]] corresponding to the list of this action. This
    * is used by the AI manager to have one "action" per [[BigGuy]].
    */
  def splitToSingleAddBigGuies: Vector[AddBigGuies.AddBigGuy] =
    idsAndPositions.map(_._1).map(AddBigGuies.AddBigGuy(_))

}

object AddBigGuies {
  final case class AddBigGuy(entityId: Entity.Id) extends GameAction.EntityCreatorAction
}
