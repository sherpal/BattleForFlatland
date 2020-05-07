package gamelogic.gamestate

import errors.ErrorADT.TooOldActionException

object ImmutableActionCollector {
  def apply(
      currentGameState: GameState,
      timeBetweenGameStates: Long = 2000,
      timeToOldestGameState: Long = 60000
  ): ImmutableActionCollector = new ImmutableActionCollector(
    currentGameState,
    List((currentGameState, Nil)),
    timeBetweenGameStates,
    timeToOldestGameState
  )
}

/**
  * Create a complete picture of the game at time T, with its full history up to `timeToOldestGameState` millis earlier.
  *
  * `actionsAndStates` remembers the [[GameState]]s from the past and the actions from one to the other.
  * An element of the list is a couple GameState, List[GameAction] where the list is sorted from oldest to
  * newest, and are the actions between that GameState and the next one. The GameStates, however, are sorted
  * from newest to oldest.
  *
  * It means that if
  * ```
  * gs1 = actionsAndStates(n)._1
  * gs2 = actionsAndStates(n-1)._1
  * actions = actionsAndStates(n)._2
  * ```
  * then we have
  * ```
  * gs2 = gs1(actions)
  * ```
  *
  * This property is maintained in the addAction method.
  *
  * @param currentGameState game state as it is now
  * @param actionsAndStates history of some [[gamelogic.gamestate.GameState]] and [[gamelogic.gamestate.GameAction]]
  *                         between them
  * @param timeBetweenGameStates time between two game states. This value should typically be of the order magnitude you
  *                               are willing to accept going back in time (with a small margin). Expressed in game
  *                               units (millis for BFF). Defaults to 2000.
  * @param timeToOldestGameState time during which you want to keep the history in memory. Expressed in game time units
  *                               (millis for BFF). Defaults to 60000.
  */
final class ImmutableActionCollector private (
    val currentGameState: GameState,
    val actionsAndStates: List[(GameState, List[GameAction])],
    val timeBetweenGameStates: Long,
    val timeToOldestGameState: Long
) {

  /**
    * Blindly add the actions, then remove all actions with ids in the list of ids to remove.
    *
    * This method should be consistent with, but more efficient than, the `masterAddAndRemoveActions`.
    * The property that must always hold is the following:
    * Given actions a_1, ..., a_n, and an action collector A,
    * val (next, oldestTime, toRemove) = A.masterAddAndRemoveActions(List(a_1, ..., a_n))
    * then
    * next must be equivalent to A.slaveAddAndRemoveActions(List(a_1, ..., a_n), oldestTime, toRemove)
    *
    * @param actionsToAdd actions to add
    * @param oldestTimeToRemove oldest time within the actions to remove
    * @param idsOfActionsToRemove list of ids of actions to remove
    */
  def slaveAddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ): ImmutableActionCollector = {
    val updatedActionsAndStates = removeActions(
      oldestTimeToRemove,
      idsOfActionsToRemove,
      actionsToAdd.foldLeft(actionsAndStates) { (asAndSs, nextAction) =>
        addAction(nextAction, asAndSs)
      }
    )

    new ImmutableActionCollector(
      updatedActionsAndStates.head._1.applyActions(updatedActionsAndStates.head._2),
      updatedActionsAndStates,
      timeBetweenGameStates,
      timeToOldestGameState
    )
  }

  /**
    * Adds new actions and remove then-illegal ones.
    * See `slaveAddAndRemoveActions` to see what property these functions must hold.
    *
    * @param actionsToAdd new actions for the [[gamelogic.gamestate.GameState]]
    * @return a triplet with the new action collector, the oldest time within actions to remove, and ids of actions to
    *         remove.
    */
  def masterAddAndRemoveActions(actionsToAdd: List[GameAction]): (ImmutableActionCollector, Long, List[GameAction.Id]) =
    if (actionsToAdd.isEmpty) {
      (this, currentGameState.time, Nil)
    } else {
      val oldestTime: Long = actionsToAdd.head.time

      @scala.annotation.tailrec
      def mergeActions(
          ls1: List[GameAction],
          ls2: List[GameAction],
          accumulator: List[GameAction]
      ): List[GameAction] =
        if (ls1.isEmpty)
          accumulator.reverse ++ ls2
        else if (ls2.isEmpty)
          accumulator.reverse ++ ls1
        else if (ls1.head.time <= ls2.head.time)
          mergeActions(ls1.tail, ls2, ls1.head +: accumulator)
        else
          mergeActions(ls1, ls2.tail, ls2.head +: accumulator)

      val actionIdsToRemove = mergeActions(
        actionsFrom(oldestTime),
        actionsToAdd.sorted,
        Nil
      ).foldLeft((gameStateUpTo(oldestTime), List[Long]())) {
          case ((state, toRemove), action) =>
            if (shouldKeepAction(action, state)) (action(state), toRemove)
            else (state, action.id +: toRemove)
        }
        ._2
        .reverse

      (
        slaveAddAndRemoveActions(actionsToAdd, oldestTime, actionIdsToRemove),
        oldestTime,
        actionIdsToRemove
      )
    }

  def removeActions(
      oldestTime: Long,
      idsToRemove: List[GameAction.Id],
      currentActionsAndStates: List[(GameState, List[GameAction])]
  ): List[(GameState, List[GameAction])] = {

    val (after, before) = currentActionsAndStates.span(_._1.time >= oldestTime)

    val (toBeChanged, toRemain) =
      if (before.nonEmpty)
        (after :+ before.head, before.tail)
      else
        (after, before)

    // after is from newest to oldest

    val reverseAfter = toBeChanged.reverse
    // reverseAfter is from oldest to newest

    val allActionsAfter = reverseAfter.flatMap(_._2)
    // allActionsAfter is from oldest to newest

    val remainingActionsAfter = allActionsAfter
      .foldLeft((List[GameAction](), idsToRemove))({
        case ((acc, toRemove), nextAction) =>
          if (toRemove.isEmpty)
            (nextAction +: acc, Nil)
          else {
            toRemove.indexOf(nextAction.id) match {
              case -1 =>
                (nextAction +: acc, toRemove)
              case idx =>
                (acc, toRemove.drop(idx + 1))
            }
          }
      })
      ._1
      .reverse
    // remainingActionsAfter is from oldest to newest

    if (remainingActionsAfter.isEmpty) {
      toRemain
    } else if (toRemain.nonEmpty) {
      (toRemain.head._1.applyActions(toRemain.head._2), remainingActionsAfter) +: toRemain
    } else {
      List((toBeChanged.last._1, remainingActionsAfter))
    }

  }

  /**
    * Adds an action to the gameState.
    * When an action is added, it is put at the last possible position, after all actions at the same time that where
    * already added.
    *
    * @param action     the [[GameAction]] to add
    */
  private def addAction(
      action: GameAction,
      currentActionsAndStates: List[(GameState, List[GameAction])]
  ): List[(GameState, List[GameAction])] =
    if (action.time > currentActionsAndStates.head._1.time + timeBetweenGameStates) {
      val temp = (currentGameState, List(action)) +: currentActionsAndStates
      if (timeToOldestGameState < temp.head._1.time - temp.last._1.time) {
        temp.dropRight(1)
      } else temp
    } else {
      def insertAction(action: GameAction, list: List[GameAction]): List[GameAction] = {
        // remaining is oldest to newest
        // treated   is newest to oldest
        @scala.annotation.tailrec
        def insertAcc(action: GameAction, treated: List[GameAction], remaining: List[GameAction]): List[GameAction] =
          if (remaining.isEmpty) (action +: treated).reverse
          else if (remaining.head > action) treated.reverse ++ (action +: remaining)
          else insertAcc(action, remaining.head +: treated, remaining.tail)

        insertAcc(action, Nil, list)
      }

      // the list actionsAndStates should typically not be too big
      // moreover, it is rare that we should add an action older than the second or third gameState,
      // provided timeToOldestGameState is not ridiculously too small...
      def add(action: GameAction, list: List[(GameState, List[GameAction])]): List[(GameState, List[GameAction])] =
        if (action.time >= list.head._1.time) (list.head._1, insertAction(action, list.head._2)) +: list.tail
        else {
          val tail = add(action, list.tail)
          // we need to update the gameState since something changed in the past
          val head = (tail.head._1.applyActions(tail.head._2), list.head._2)
          head +: tail
        }

      try {
        add(action, currentActionsAndStates)
      } catch {
        case e: Throwable =>
          println("gs time " + currentActionsAndStates.head._1.time)
          println("action time " + action.time)
          throw e
      }
    }

  /**
    * Returns the GameState as it was at time time.
    */
  private def gameStateUpTo(time: Long): GameState = actionsAndStates.find(_._1.time <= time) match {
    case Some((gs, actions)) =>
      gs.applyActions(actions.takeWhile(_.time <= time))
    case None =>
      println("beuh", time)
      throw TooOldActionException(time.toString)
  }

  private def actionsFrom(time: Long): List[GameAction] =
    actionsAndStates
      .take(actionsAndStates.indexWhere(_._1.time < time) + 1)
      .reverse
      .flatMap(_._2)
      .dropWhile(_.time <= time)

  private def shouldKeepAction(action: GameAction, state: GameState): Boolean = state.isLegalAction(action)

}
