package gamelogic.gamestate.serveractions

import gamelogic.buffs.TickerBuff
import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.TickerBuffTicks
import gamelogic.utils.{AbilityUseIdGenerator, BuffIdGenerator, EntityIdGenerator, GameActionIdGenerator}

/**
  * This [[gamelogic.gamestate.serveractions.ServerAction]] triggers the actions of all the ticker buffs in the game
  * for which their last tick was further back in time than their tick rate.
  *
  * We set the time for their actions at the last tick plus the tick rate, so that the ticks are at the exact times and
  * without delays.
  */
final class ManageTickerBuffs extends ServerAction {
  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit gameActionIdGenerator: GameActionIdGenerator,
      entityIdGenerator: EntityIdGenerator,
      abilityUseIdGenerator: AbilityUseIdGenerator,
      buffIdGenerator: BuffIdGenerator
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val tickActions = gameState.buffs
      .flatMap(_._2)
      .values
      .collect {
        case ticker: TickerBuff if startTime - ticker.lastTickTime > ticker.tickRate => ticker
      }
      .flatMap(
        ticker =>
          TickerBuffTicks(0L, ticker.lastTickTime + ticker.tickRate, ticker.buffId, ticker.bearerId) +: ticker
            .tickEffect(gameState, startTime, entityIdGenerator)
      )
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(tickActions)

    (nextCollector, ServerAction.ServerActionOutput(tickActions, oldestTime, idsToRemove))
  }
}
