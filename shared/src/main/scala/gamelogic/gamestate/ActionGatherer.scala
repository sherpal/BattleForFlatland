package gamelogic.gamestate

trait ActionGatherer {

  val currentGameState: GameState

  /** Blindly add the actions, then remove all actions with ids in the list of ids to remove.
    *
    * This method should be consistent with, but more efficient than, the
    * `masterAddAndRemoveActions`. The property that must always hold is the following: Given
    * actions a_1, ..., a_n, and an action collector A, val (next, oldestTime, toRemove) =
    * A.masterAddAndRemoveActions(List(a_1, ..., a_n)) then next must be equivalent to
    * A.slaveAddAndRemoveActions(List(a_1, ..., a_n), oldestTime, toRemove)
    *
    * @param actionsToAdd
    *   actions to add
    * @param oldestTimeToRemove
    *   oldest time within the actions to remove
    * @param idsOfActionsToRemove
    *   list of ids of actions to remove
    */
  def slaveAddAndRemoveActions(
      actionsToAdd: Vector[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: Vector[GameAction.Id]
  ): ActionGatherer

  /** Adds new actions and remove then-illegal ones. See `slaveAddAndRemoveActions` to see what
    * property these functions must hold.
    *
    * @param actionsToAdd
    *   new actions for the [[gamelogic.gamestate.GameState]]
    * @return
    *   a triplet with the new action collector, the oldest time within actions to remove, and ids
    *   of actions to remove.
    */
  def masterAddAndRemoveActions(
      actionsToAdd: Vector[GameAction]
  ): (ActionGatherer, Long, Vector[GameAction.Id])

  protected final def shouldKeepAction(action: GameAction, state: GameState): Boolean =
    state.isLegalAction(action)

}
