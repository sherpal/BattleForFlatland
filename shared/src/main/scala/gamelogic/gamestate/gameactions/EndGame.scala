package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{EdgeGameTransformer, GameStateTransformer}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Simply ends the game. */
final case class EndGame(id: GameAction.Id, time: Long) extends GameAction {

  def isLegal(gameState: GameState)(using i18n: I18n): Option[String] =
    Option.when(gameState.ended)(i18n.gameAlreadyEnded)

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new EdgeGameTransformer(time, EdgeGameTransformer.EdgeType.Ending)
}
