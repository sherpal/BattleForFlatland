package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.Drinking
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** The Spirit of Nara starts drinking (see [[Drinking]]). */
final case class PutDrinking(id: GameAction.Id, time: Long, buffId: Buff.Id, bossId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    WithBuff(Drinking(buffId, bossId, time, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutDrinking = copy(id = newId)
}
