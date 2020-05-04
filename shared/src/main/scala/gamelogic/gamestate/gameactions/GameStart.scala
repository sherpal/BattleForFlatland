package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EdgeGameTransformer, GameStateTransformer}
import gamelogic.gamestate.{GameAction, GameState}

/** Simply starts the game. */
final case class GameStart(id: Long, time: Long) extends GameAction {

  def isLegal(gameState: GameState): Boolean = !gameState.started

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer = new EdgeGameTransformer(
    time,
    EdgeGameTransformer.EdgeType.Beginning
  )
}
