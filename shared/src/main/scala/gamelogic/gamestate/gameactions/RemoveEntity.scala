package gamelogic.gamestate.gameactions

import cats.kernel.Monoid
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{
  GameStateTransformer,
  RemoveBuffTransformer,
  RemoveEntityTransformer
}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

final case class RemoveEntity(id: GameAction.Id, time: Long, entityId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    RemoveEntityTransformer(entityId, time) ++
      Monoid[GameStateTransformer].combineAll(
        gameState.allBuffs
          .filter(_.bearerId == entityId)
          .map(buff => RemoveBuffTransformer(time, entityId, buff.buffId))
      )
  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): GameAction = copy(id = newId)
}
