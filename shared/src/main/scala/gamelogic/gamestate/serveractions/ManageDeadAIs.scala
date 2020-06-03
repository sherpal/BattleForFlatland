package gamelogic.gamestate.serveractions

import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.utils.IdGeneratorContainer

final class ManageDeadAIs extends ServerAction {
  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val now = nowGenerator()

    val actions = currentState.currentGameState.bosses
      .filter(_._2.life <= 0)
      .keys
      .map(
        RemoveEntity(
          idGeneratorContainer.gameActionIdGenerator(),
          now,
          _
        )
      )
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }

}
