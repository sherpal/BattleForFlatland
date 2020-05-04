package gamelogic.gamestate.statetransformers

import gamelogic.entities.EntityCastingInfo
import gamelogic.gamestate.GameState

/** Makes the given entity to start casting an ability.  */
final class EntityStartsCastingTransformer(entityCastingInfo: EntityCastingInfo) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(
      time              = entityCastingInfo.startedTime,
      castingEntityInfo = gameState.castingEntityInfo + (entityCastingInfo.casterId -> entityCastingInfo)
    )
}
