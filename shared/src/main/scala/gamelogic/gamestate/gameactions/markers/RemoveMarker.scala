package gamelogic.gamestate.gameactions.markers

import gamelogic.gamestate.GameAction
import gamelogic.gameextras.GameMarker
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.{GameStateTransformer, RemoveMarkerTransformer}
import gamelogic.gamestate.GameState

/**
  * Removes the [[GameMarker]] from the map.
  */
final case class RemoveMarker(id: GameAction.Id, time: Long, marker: GameMarker) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    RemoveMarkerTransformer(marker)

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
