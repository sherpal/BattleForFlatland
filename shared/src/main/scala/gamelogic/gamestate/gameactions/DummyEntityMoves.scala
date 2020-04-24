package gamelogic.gamestate.gameactions

import gamelogic.entities.{DummyLivingEntity, Entity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class DummyEntityMoves(
    id: GameAction.Id,
    time: Long,
    playerId: Entity.Id,
    newPos: Complex,
    moving: Boolean,
    direction: Double
) extends GameAction {

  /**
    * If that player exists, moves it to the new given position, direction, and moving state.
    */
  def apply(gameState: GameState): GameState =
    gameState.players
      .get(playerId)
      .fold(gameState)(
        player =>
          gameState.withPlayer(
            time,
            player.copy(
              pos       = newPos,
              direction = direction,
              moving    = moving
            )
          )
      )

}
