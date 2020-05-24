package gamelogic.gamestate.gameactions

import gamelogic.buffs.{BasicShield, Buff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

final case class PutBasicShield(id: GameAction.Id, time: Long, buffId: Buff.Id, bearerId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithBuff(BasicShield(buffId, bearerId, time))

  def isLegal(gameState: GameState): Boolean = gameState.entityById(bearerId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
