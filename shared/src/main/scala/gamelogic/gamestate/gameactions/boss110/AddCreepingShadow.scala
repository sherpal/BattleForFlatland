package gamelogic.gamestate.gameactions.boss110

import gamelogic.gamestate.GameAction
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.GameStateTransformer
import gamelogic.gamestate.GameState
import gamelogic.gamestate.statetransformers.WithEntity
import gamelogic.entities.boss.boss110.CreepingShadow
import gamelogic.physics.Complex
import gamelogic.entities.boss.dawnoftime.Boss110

/**
  * [[GameAction]] adding a instance of [[CreepingShadow]] to the game.
  *
  * @param id id of the action
  * @param time time of the action
  * @param entityId id of the [[CreepingShadow]]
  * @param sourceId id of the boss.
  */
final case class AddCreepingShadow(id: GameAction.Id, time: Long, entityId: Entity.Id, sourceId: Entity.Id)
    extends GameAction
    with GameAction.EntityCreatorAction {

  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithEntity(
      CreepingShadow(entityId, time, Complex(-Boss110.halfWidth, 0), 1.0, sourceId, 0.0, moving = false, Map.empty),
      time
    )

  def isLegal(gameState: GameState): Option[String] = Option.empty

  def changeId(newId: GameAction.Id): GameAction = copy(id = newId)

}
