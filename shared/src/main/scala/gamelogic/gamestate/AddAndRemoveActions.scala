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

  def composeWithNext(that: AddAndRemoveActions): AddAndRemoveActions = AddAndRemoveActions(
    this.actionsToAdd ++ that.actionsToAdd,
    this.oldestTimeToRemove min that.oldestTimeToRemove,
    this.idsOfActionsToRemove ++ that.idsOfActionsToRemove
  )

  /** The same update, without the actions that it adds only to remove them right away (those that
    * the game master refused). Receivers drop them anyway, but bots propose many, and this saves
    * sending and decoding them.
    */
  def withoutRefusedActions: AddAndRemoveActions = {
    val idsToRemove = idsOfActionsToRemove.toSet
    val addedIds    = actionsToAdd.map(_.id).toSet
    AddAndRemoveActions(
      actionsToAdd.filterNot(action => idsToRemove.contains(action.id)),
      oldestTimeToRemove,
      idsOfActionsToRemove.filterNot(addedIds.contains)
    )
  }
}

object AddAndRemoveActions {
  val empty = AddAndRemoveActions(Vector.empty, Long.MaxValue, Vector.empty)
}
