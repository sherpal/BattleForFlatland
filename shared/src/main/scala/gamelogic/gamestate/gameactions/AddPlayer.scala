package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddPlayer(
    id: GameAction.Id,
    time: Long,
    playerId: Entity.Id,
    pos: Complex,
    colour: Int
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    ???

  def isLegal(gameState: GameState): Option[String] =
    Option.when(gameState.players.isDefinedAt(playerId))("Player already exists")

  def changeId(newId: Id): GameAction = copy(id = newId)
}
