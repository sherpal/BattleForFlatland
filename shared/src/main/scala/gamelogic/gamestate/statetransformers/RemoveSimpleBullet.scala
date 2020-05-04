package gamelogic.gamestate.statetransformers
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState

/** Removes a [[gamelogic.entities.SimpleBulletBody]] from the game. */
final class RemoveSimpleBullet(time: Long, bulletId: Entity.Id) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = gameState.copy(
    time          = time,
    simpleBullets = gameState.simpleBullets - bulletId
  )
}
