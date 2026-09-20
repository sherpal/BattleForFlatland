package gamelogic.gamestate.gameactions.boss104

import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.DeathMarkTriangle
import gamelogic.gamestate.GameAction.{EntityCreatorAction, Id}
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

/** Creates the [[DeathMarkTriangle]] once the third big guy has died.
  *
  * The three positions are carried by the action (and not looked up in the game state) so that the
  * transformer stays pure: this action is emitted by
  * [[gamelogic.buffs.boss.boss104.BigGuyCurse]] on the game master, together with the third
  * [[gamelogic.entities.boss.boss104.BigGuyDeathMark]] itself.
  */
final case class AddDeathMarkTriangle(
    id: GameAction.Id,
    time: Long,
    entityId: Entity.Id,
    markPositions: Vector[Complex]
) extends GameAction
    with EntityCreatorAction {

  override def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithEntity(DeathMarkTriangle.fromMarkPositions(entityId, time, markPositions), time)

  override def isLegal(gameState: GameState): Option[String] = None

  override def changeId(newId: Id): GameAction = copy(id = newId)
}
