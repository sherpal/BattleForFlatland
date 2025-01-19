package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EdgeGameTransformer, GameStateTransformer}
import gamelogic.gamestate.{GameAction, GameState}

/** Simply ends the game. */
final case class EndGame(id: GameAction.Id, time: Long) extends GameAction {

  def isLegal(gameState: GameState): Option[String] =
    Option.when(gameState.ended)("Game has already ended")

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new EdgeGameTransformer(time, EdgeGameTransformer.EdgeType.Ending)
}
