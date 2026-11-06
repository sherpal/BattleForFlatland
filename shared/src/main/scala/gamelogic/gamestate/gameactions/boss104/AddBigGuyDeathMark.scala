package gamelogic.gamestate.gameactions.boss104

import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.BigGuyDeathMark
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithEntity}
import gamelogic.physics.Complex
import services.i18n.I18n

case class AddBigGuyDeathMark(id: GameAction.Id, time: Long, entityId: Entity.Id, position: Complex)
    extends GameAction
    with EntityCreatorAction {
  override def createGameStateTransformer(gameState: GameState): GameStateTransformer = WithEntity(
    BigGuyDeathMark(entityId, time, position),
    time
  )

  override def isLegal(gameState: GameState)(using I18n): Option[String] = None

  override def changeId(newId: GameAction.Id): GameAction = copy(id = newId)
}
