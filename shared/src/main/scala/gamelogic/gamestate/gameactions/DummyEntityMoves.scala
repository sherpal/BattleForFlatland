package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class DummyEntityMoves(
    id: GameAction.Id,
    time: Long,
    playerId: Entity.Id,
    newPos: Complex,
    moving: Boolean,
    direction: Double,
    colour: Int
) extends GameAction {

  /**
    * If that player exists, moves it to the new given position, direction, and moving state.
    */
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.players.get(playerId).fold(GameStateTransformer.identityTransformer) { player =>
      ???
    //new WithPlayer(player.copy(time = time, pos = newPos, direction = direction, moving = moving))
    }

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
