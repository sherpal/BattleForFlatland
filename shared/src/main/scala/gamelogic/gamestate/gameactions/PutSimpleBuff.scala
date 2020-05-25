package gamelogic.gamestate.gameactions

import gamelogic.buffs.{Buff, SimpleBuffs}
import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

final case class PutSimpleBuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    appearanceTime: Long,
    resourceIdentifier: ResourceIdentifier
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.entityById(bearerId).fold(GameStateTransformer.identityTransformer) { _ =>
      new WithBuff(SimpleBuffs(resourceIdentifier, buffId, bearerId, appearanceTime).get)
    }

  def isLegal(gameState: GameState): Boolean = gameState.entityById(bearerId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
