package gamelogic.gamestate.serveractions

import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.TickerBuffTicks
import gamelogic.utils.IdGeneratorContainer

/**
  * This [[gamelogic.gamestate.serveractions.ServerAction]] triggers the actions of all the ticker buffs in the game
  * for which their last tick was further back in time than their tick rate.
  *
  * We set the time for their actions at the last tick plus the tick rate, so that the ticks are at the exact times and
  * without delays.
  */
final class ManageTickerBuffs extends ServerAction {
  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val tickActions = gameState.tickerBuffs
      .flatMap(_._2)
      .values
      .filter(ticker => startTime - ticker.lastTickTime >= ticker.tickRate)
      .flatMap(
        ticker =>
          TickerBuffTicks(
            idGeneratorContainer.gameActionIdGenerator(),
            ticker.lastTickTime + ticker.tickRate,
            ticker.buffId,
            ticker.bearerId
          ) +: ticker
            .tickEffect(gameState, startTime, idGeneratorContainer)
      )
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(tickActions)

    (nextCollector, ServerAction.ServerActionOutput(tickActions, oldestTime, idsToRemove))
  }
}
