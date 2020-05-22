package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddPlayer(id: Long, time: Long, playerId: Entity.Id, pos: Complex, colour: Int) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    ???

  def isLegal(gameState: GameState): Boolean = !gameState.players.isDefinedAt(playerId)

  def changeId(newId: Id): GameAction = copy(id = newId)
}
