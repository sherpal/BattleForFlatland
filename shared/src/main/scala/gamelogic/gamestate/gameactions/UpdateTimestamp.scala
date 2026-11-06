package gamelogic.gamestate.gameactions

import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, UpdateTimeTransformer}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

final case class UpdateTimestamp(id: GameAction.Id, time: Long) extends GameAction {

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    UpdateTimeTransformer(time)
}
