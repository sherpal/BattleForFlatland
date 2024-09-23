package gamelogic.gamestate.statetransformers

import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.entities.classes.PlayerClass

final class WithEntity(entity: Entity, time: Long) extends GameStateTransformer {
  def apply(gameState: GameState): GameState =
    gameState.copy(
      time = time,
      entities = gameState.entities + (entity.id -> entity),
      deadPlayers = entity match {
        case player: PlayerClass =>
          gameState.deadPlayers + (player.id -> player)
        case _ => gameState.deadPlayers
      }
    )
}
