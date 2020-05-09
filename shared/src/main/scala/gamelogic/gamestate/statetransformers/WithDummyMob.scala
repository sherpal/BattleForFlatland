package gamelogic.gamestate.statetransformers
import gamelogic.entities.DummyMob
import gamelogic.gamestate.GameState

final class WithDummyMob(mob: DummyMob) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(time = mob.time, dummyMobs = gameState.dummyMobs + (mob.id -> mob))
}
