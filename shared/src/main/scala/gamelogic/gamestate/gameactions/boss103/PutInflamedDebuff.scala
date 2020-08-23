package gamelogic.gamestate.gameactions.boss103

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss103.Inflamed
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

/** Puts the [[gamelogic.buffs.boss.boss103.Inflamed]] on the target. */
final case class PutInflamedDebuff(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithBuff(
      Inflamed(buffId = buffId, bearerId = bearerId, appearanceTime = time, lastTickTime = time, sourceId = sourceId)
    )

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): PutInflamedDebuff = copy(id = newId)
}
