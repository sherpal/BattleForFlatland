package gamelogic.gamestate.statetransformers

import gamelogic.gameextras.GameMarker
import gamelogic.gamestate.GameState

final class RemoveMarkerTransformer private (marker: GameMarker) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = gameState.removeMarkerInfo(marker)
}

object RemoveMarkerTransformer {

  private val removeMarkerTransformerMap: Map[GameMarker, GameStateTransformer] = 
    GameMarker.allMarkers.map(marker => marker -> new RemoveMarkerTransformer(marker)).toMap

  def apply(marker: GameMarker): GameStateTransformer = 
    removeMarkerTransformerMap(marker)

}
