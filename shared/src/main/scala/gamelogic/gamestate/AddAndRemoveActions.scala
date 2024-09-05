package gamelogic.gamestate

/** Some kind of "meta" action that tells what actions have been added to the game state, and what
  * actions are removed
  */
final case class AddAndRemoveActions(
    actionsToAdd: Vector[GameAction],
    oldestTimeToRemove: Long,
    idsOfActionsToRemove: Vector[GameAction.Id]
) {
  def nonEmpty: Boolean = actionsToAdd.nonEmpty || idsOfActionsToRemove.nonEmpty
}
