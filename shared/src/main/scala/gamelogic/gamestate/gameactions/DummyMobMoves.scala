package gamelogic.gamestate.gameactions

import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithDummyMob}
import gamelogic.physics.Complex

final case class DummyMobMoves(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    position: Complex,
    direction: Double,
    rotation: Double,
    moving: Boolean
) extends GameAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.dummyMobs.get(entityId).fold(GameStateTransformer.identityTransformer) { mob =>
      new WithDummyMob(
        mob.copy(time = time, pos = position, direction = direction, rotation = rotation, moving = moving)
      )
    }

  /**
    * Checks that the new position is not too far away from the current position position.
    * We allow 10% of inaccuracy due to possible delay.
    */
  def isLegal(gameState: GameState): Boolean =
    gameState.dummyMobs
      .get(entityId)
      .forall(
        mob => (mob.currentPosition(gameState.time) - position).modulus < mob.speed * 1.1 * (time - gameState.time)
      )

  def changeId(newId: Id): GameAction = copy(id = newId)
}
