package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, UpdateTimeTransformer}
import gamelogic.gamestate.{GameAction, GameState}

final case class UpdateTimestamp(id: GameAction.Id, time: Long) extends GameAction {

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    UpdateTimeTransformer(time)
}
