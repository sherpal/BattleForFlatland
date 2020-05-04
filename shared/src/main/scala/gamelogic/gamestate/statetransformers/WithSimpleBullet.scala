package gamelogic.gamestate.statetransformers
import gamelogic.entities.SimpleBulletBody
import gamelogic.gamestate.GameState

/** Adds (or updates) the given [[gamelogic.entities.SimpleBulletBody]] in the game. */
final class WithSimpleBullet(simpleBullet: SimpleBulletBody) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(
      time          = simpleBullet.time,
      simpleBullets = gameState.simpleBullets + (simpleBullet.id -> simpleBullet)
    )
}
