package gamelogic.gamestate.gameactions

import gamelogic.buffs.{BasicShield, Buff, RageFiller}
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

final case class PutSquareDefaultBuffs(id: GameAction.Id, time: Long, buffIds: (Buff.Id, Buff.Id), bearerId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithBuff(BasicShield(buffIds._1, bearerId, time)) ++
      new WithBuff(RageFiller(buffIds._2, bearerId, time))

  def isLegal(gameState: GameState): Boolean = gameState.entityById(bearerId).isDefined

  def changeId(newId: Id): GameAction = copy(id = newId)
}
