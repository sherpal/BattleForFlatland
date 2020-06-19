package gamelogic.gamestate.statetransformers

import gamelogic.abilities.Ability
import gamelogic.gamestate.GameState

final class CasterUsesAbility(ability: Ability) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState
      .withAbilityEntitiesById(ability.casterId)
      .fold {
        gameState
      } { entity =>
        gameState.copy(
          time              = ability.time,
          castingEntityInfo = gameState.castingEntityInfo - entity.id,
          entities          = gameState.entities + (entity.id -> entity.useAbility(ability))
        )
      }
}
