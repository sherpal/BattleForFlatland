package gamelogic.gamestate.gameactions

import gamelogic.entities.{DummyMob, Entity}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

final case class AddDummyMob(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithEntity(DummyMob(entityId, time, position, DummyMob.speed, moving = false, 0.0, 0.0), time = time)

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
