package gamelogic.gamestate.gameactions

import gamelogic.buffs.{Buff, TickerBuff}
import gamelogic.entities.Entity
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}

/**
  * Each time a ticker buff ticks, we need to change the last time it actually ticked.
  */
final case class TickerBuffTicks(id: GameAction.Id, time: Long, buffId: Buff.Id, bearerId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    gameState.buffs
      .get(bearerId)
      .flatMap(_.get(buffId))
      .collect { case ticker: TickerBuff => ticker.changeLastTickTime(time) }
      .fold(GameStateTransformer.identityTransformer) { buff =>
        new WithBuff(buff)
      }

  def isLegal(gameState: GameState): Boolean =
    gameState.buffs
      .get(bearerId)
      .flatMap(_.get(buffId))
      .exists(_.isInstanceOf[TickerBuff])

  def changeId(newId: Id): GameAction = copy(id = newId)
}
