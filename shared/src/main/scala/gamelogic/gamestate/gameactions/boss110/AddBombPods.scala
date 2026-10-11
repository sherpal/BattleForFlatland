package gamelogic.gamestate.gameactions.boss110

import cats.kernel.Monoid
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.physics.Complex

/** Puts a [[BombPod]] on the game for each elements of the `idsAndPositions` [[List]].
  */
final case class AddBombPods(
    id: GameAction.Id,
    time: Long,
    idsAndPositions: List[(Entity.Id, Complex)],
    powderMonkeyId: Entity.Id
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    Monoid[GameStateTransformer].combineAll(
      idsAndPositions.map { case (entityId, position) =>
        new WithEntity(BombPod(entityId, time, position, powderMonkeyId), time)
      }
    )

  def isLegal(gameState: GameState): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
