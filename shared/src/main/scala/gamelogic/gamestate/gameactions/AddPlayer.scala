package gamelogic.gamestate.gameactions

import gamelogic.entities.{DummyLivingEntity, Entity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddPlayer(id: Long, time: Long, playerId: Entity.Id, pos: Complex, colour: String) extends GameAction {

  override def apply(gameState: GameState): GameState = gameState.withPlayer(
    time,
    DummyLivingEntity(playerId, pos, 0, moving = false, 100, colour)
  )
}
