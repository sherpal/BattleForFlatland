package gamelogic.gamestate.statetransformers

import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.GameState

/** Adds (or modifies) the given player at the given time. */
final class WithPlayer(player: PlayerClass) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(time = player.time, players = gameState.players + (player.id -> player))
}
