package gamelogic.gamestate

final class GreedyActionGatherer(val currentGameState: GameState) extends ActionGatherer {

  def slaveAddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ): GreedyActionGatherer =
    new GreedyActionGatherer(
      currentGameState.applyActions(actionsToAdd.filterNot(action => idsOfActionsToRemove.contains(action.id)))
    )

  def masterAddAndRemoveActions(actionsToAdd: List[GameAction]): (GreedyActionGatherer, Long, List[GameAction.Id]) = {
    val (newGameState, idsToRemove) = actionsToAdd.foldLeft((currentGameState, List.empty[GameAction.Id])) {
      case ((gameState, idsToRemove), action) =>
        if (shouldKeepAction(action, gameState)) (action(gameState), idsToRemove)
        else (gameState, action.id +: idsToRemove)
    }

    (new GreedyActionGatherer(newGameState), 0L, idsToRemove)
  }

}
