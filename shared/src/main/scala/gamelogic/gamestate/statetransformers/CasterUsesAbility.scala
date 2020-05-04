package gamelogic.gamestate.statetransformers
import gamelogic.abilities.Ability
import gamelogic.entities.DummyLivingEntity
import gamelogic.gamestate.GameState

final class CasterUsesAbility(ability: Ability) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.withAbilityEntitiesById(ability.casterId).map(_.useAbility(ability)) match {
      case Some(player: DummyLivingEntity) =>
        gameState.copy(
          time              = ability.time,
          castingEntityInfo = gameState.castingEntityInfo - player.id,
          players           = gameState.players + (player.id -> player)
        )
      case _ => gameState
    }
}
