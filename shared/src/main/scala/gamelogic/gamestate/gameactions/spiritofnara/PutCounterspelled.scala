package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.Counterspelled
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Puts a [[Counterspelled]] on the player. */
final case class PutCounterspelled(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(Counterspelled(buffId, bearerId, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutCounterspelled = copy(id = newId)
}
