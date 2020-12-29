package gamelogic.gamestate.gameactions.markers

import gamelogic.gamestate.GameAction
import gamelogic.gamestate.statetransformers.{GameStateTransformer, UpdateMarkerTransformer}
import gamelogic.gamestate.GameState
import gamelogic.gameextras.GameMarkerInfo

/**
  * Update or Create the marker specified in the [[GameMarkerInfo]].
  */
final case class UpdateMarker(id: GameAction.Id, time: Long, gameMarkerInfo: GameMarkerInfo) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer = 
    new UpdateMarkerTransformer(gameMarkerInfo)

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

  def isLegal(gameState: GameState): Boolean = true
}