package gamelogic.gamestate.gameactions.spiritofnara

import gamelogic.buffs.Buff
import gamelogic.buffs.boss.spiritofnara.Chilled
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, RemoveBuffTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}
import services.i18n.I18n

/** Puts a [[Chilled]] on the bearer, replacing the one it may already have.
  *
  * Should be followed by a [[gamelogic.gamestate.gameactions.RefreshMovementSpeed]].
  */
final case class PutChilled(id: GameAction.Id, time: Long, buffId: Buff.Id, bearerId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState
      .allBuffsOfEntity(bearerId)
      .collect { case buff: Chilled => buff.buffId }
      .toVector
      .sortBy(_.value)
      .map(previous => RemoveBuffTransformer(time, bearerId, previous): GameStateTransformer)
      .foldLeft(GameStateTransformer.identityTransformer)(_ ++ _) ++
      WithBuff(Chilled(buffId, bearerId, time))

  def isLegal(gameState: GameState)(using I18n): None.type = None

  def changeId(newId: Id): PutChilled = copy(id = newId)
}
