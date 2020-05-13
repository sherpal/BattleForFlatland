package gamelogic.gamestate.statetransformers

import gamelogic.entities.{DummyLivingEntity, DummyMob, Entity}
import gamelogic.gamestate.GameState

final class WithEntity(entity: Entity) extends GameStateTransformer {
  def apply(gameState: GameState): GameState = entity match {
    case entity: DummyLivingEntity =>
      gameState.copy(time = entity.time, players = gameState.players + (entity.id -> entity))
    case entity: DummyMob => gameState.copy(time = entity.time, dummyMobs = gameState.dummyMobs + (entity.id -> entity))
  }
}
