package gamelogic.gamestate.gameactions.boss105

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.boss105.Yearning
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Puts the [[Yearning]] buff on the Shade. */
final case class PutYearning(
    id: GameAction.Id,
    time: Long,
    buffId: Buff.Id,
    bearerId: Entity.Id,
    sourceId: Entity.Id
) extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(Yearning(buffId, bearerId, sourceId, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutYearning = copy(id = newId)
}
