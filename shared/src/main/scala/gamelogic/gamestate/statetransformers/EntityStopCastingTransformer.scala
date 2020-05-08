package gamelogic.gamestate.statetransformers
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState

/**
  * The given entity stop casting their spell, typically before the ability.
  *
  * This can happen if the entity moves, or if their cast is interrupted for some reason.
  */
final class EntityStopCastingTransformer(entityId: Entity.Id) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(castingEntityInfo = gameState.castingEntityInfo - entityId)
}
