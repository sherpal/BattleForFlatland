package gamelogic.gamestate

/**
  * Some kind of "meta" action that tells what actions have been added to the game state, and what actions are removed
  */
case class AddAndRemoveActions(
    actionsToAdd: List[GameAction],
    oldestTimeToRemove: Long,
    idsOfActionsToRemove: List[GameAction.Id]
)
