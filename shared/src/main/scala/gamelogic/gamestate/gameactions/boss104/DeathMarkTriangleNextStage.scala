package gamelogic.gamestate.gameactions.boss104

import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.DeathMarkTriangle
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}

/** Moves the [[DeathMarkTriangle]] to its next stage, flipping which side of it is deadly.
  *
  * Reading the entity from the game state is fine here (and keeps the action tiny): every client
  * has the very same triangle at that point in the timeline, so the transformer stays
  * deterministic.
  */
final case class DeathMarkTriangleNextStage(
    id: GameAction.Id,
    time: Long,
    triangleId: Entity.Id
) extends GameAction {

  override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .entityByIdAs[DeathMarkTriangle](triangleId)
      .fold(GameStateTransformer.identityTransformer)(triangle =>
        WithEntity(triangle.nextStage, time)
      )

  override def isLegal(gameState: GameState): Option[String] = None

  override def changeId(newId: Id): GameAction = copy(id = newId)
}
