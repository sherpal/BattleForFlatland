package gamelogic.gamestate.serveractions
import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.utils.IdGeneratorContainer

final class ManageDeadPlayers extends ServerAction {
  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val now = nowGenerator()

    val actions = currentState.currentGameState.players
      .filter(_._2.life <= 0)
      .keys
      .map(RemoveEntity(genActionId(), now, _))
      .toVector

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }

}
