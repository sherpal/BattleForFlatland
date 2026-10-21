package gamelogic.gamestate.gameactions.boss105

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss105.VolatileCharge
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

/** Puts a [[VolatileCharge]] on the bearer. */
final case class PutVolatileCharge(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(VolatileCharge(buffId, bearerId, sourceId, time))

  def isLegal(gameState: GameState): None.type = None

  def changeId(newId: Id): PutVolatileCharge = copy(id = newId)
}
