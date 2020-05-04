package gamelogic.gamestate.statetransformers
import gamelogic.entities.DummyLivingEntity
import gamelogic.gamestate.GameState

/** Adds (or modifies) the given player at the given time. */
final class WithPlayer(player: DummyLivingEntity) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(time = player.time, players = gameState.players + (player.id -> player))
}
