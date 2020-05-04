package gamelogic.gamestate.gameactions

import gamelogic.entities.{DummyLivingEntity, Entity}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithPlayer}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddPlayer(id: Long, time: Long, playerId: Entity.Id, pos: Complex, colour: Int) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithPlayer(DummyLivingEntity(playerId, time, pos, 0, moving = false, 100, colour, Map()))

  def isLegal(gameState: GameState): Boolean = !gameState.players.isDefinedAt(playerId)

  def changeId(newId: Id): GameAction = copy(id = newId)
}
