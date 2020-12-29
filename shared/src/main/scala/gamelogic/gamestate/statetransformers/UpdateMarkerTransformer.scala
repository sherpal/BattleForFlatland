package gamelogic.gamestate.statetransformers

import gamelogic.gameextras.GameMarkerInfo
import gamelogic.gamestate.GameState

final class UpdateMarkerTransformer(gameMarkerInfo: GameMarkerInfo) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = gameState.withMarkerInfo(gameMarkerInfo)
}
