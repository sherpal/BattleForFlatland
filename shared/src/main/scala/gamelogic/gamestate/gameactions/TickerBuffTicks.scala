package gamelogic.gamestate.gameactions

import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.gamestate.GameAction.Id
import gamelogic.gamestate.statetransformers.{GameStateTransformer, WithBuff}
import gamelogic.gamestate.{GameAction, GameState}

/**
  * Each time a ticker buff ticks, we need to change the last time it actually ticked.
  */
final case class TickerBuffTicks(id: GameAction.Id, time: Long, buffId: Buff.Id, bearerId: Entity.Id)
    extends GameAction {
  def createGameStateTransformer(gameState: GameState): GameStateTransformer =
    (for {
      bearerBuffs <- gameState.tickerBuffs.get(bearerId)
      buff        <- bearerBuffs.get(buffId)
      tickedBuff = buff.changeLastTickTime(time)
    } yield tickedBuff).fold(GameStateTransformer.identityTransformer)(new WithBuff(_))

  def isLegal(gameState: GameState): Option[String] =
    (for {
      bearerBuffs <- gameState.tickerBuffs.get(bearerId)
      _           <- bearerBuffs.get(buffId)
    } yield ()) match {
      case None => Some(s"The buff $buffId on entity $bearerId does not exist")
      case _    => None
    }

  def changeId(newId: Id): GameAction = copy(id = newId)
}
