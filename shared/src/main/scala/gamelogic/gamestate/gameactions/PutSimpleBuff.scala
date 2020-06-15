package gamelogic.gamestate.gameactions

import gamelogic.buffs.Buff.ResourceIdentifier
import gamelogic.buffs.{Buff, SimpleBuffs}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

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
      val newBuff = SimpleBuffs(resourceIdentifier, buffId, bearerId, appearanceTime).get
      new WithBuff(newBuff)
    }

  def isLegal(gameState: GameState): Boolean = gameState.entityById(bearerId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
