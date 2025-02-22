package gamelogic.gamestate.serveractions
import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.EndGame
import gamelogic.utils.IdGeneratorContainer

final class ManageEndOfGame extends ServerAction {
  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val actions = Option(
      currentState.currentGameState.started && !currentState.currentGameState.ended
    )
      .filter(
        _ && (currentState.currentGameState.players.isEmpty || currentState.currentGameState.bosses.isEmpty)
      )
      .map(_ => EndGame(genActionId(), nowGenerator()))
      .toVector

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }
}
