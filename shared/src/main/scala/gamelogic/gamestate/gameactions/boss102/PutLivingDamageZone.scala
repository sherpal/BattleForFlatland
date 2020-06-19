package gamelogic.gamestate.gameactions.boss102

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

/**
  * Puts the [[gamelogic.buffs.boss.boss102.LivingDamageZone]] debuff on the `bearerId` target.
  */
final case class PutLivingDamageZone(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    damage: Double,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    new WithBuff(LivingDamageZone(buffId, bearerId, time, time, damage, sourceId))

  def isLegal(gameState: GameState): Boolean = true

  def changeId(newId: Id): GameAction = copy(id = newId)
}
