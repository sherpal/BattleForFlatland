package gamelogic.gamestate.gameactions.markers

import gamelogic.gameextras.GameMarker
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, RemoveMarkerTransformer}
import services.i18n.I18n

/** Removes the [[GameMarker]] from the map.
  */
final case class RemoveMarker(id: GameAction.Id, time: Long, marker: GameMarker)
    extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    RemoveMarkerTransformer(marker)

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
