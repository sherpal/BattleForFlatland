package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EntityStopCastingTransformer, GameStateTransformer}

final case class EntityCastingInterrupted(id: GameAction.Id, time: Long, entityId: Entity.Id) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new EntityStopCastingTransformer(entityId)

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
