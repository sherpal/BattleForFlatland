package gamelogic.gamestate

import errors.ErrorADT.TooOldActionException

/**
  * An ActionCollector will gather all [[GameAction]]s, sort then in order and allow to recover [[GameState]]s.
  *
  * actionsAndStates remembers the [[GameState]]s from the past and the actions from one to the other.
  * An element of the list is a couple GameState, List[GameAction] where the list is sorted from oldest to
  * newest, and are the actions between that GameState and the next one. The GameStates, however, are sorted
  * from newest to oldest.
  *
  * It means that if
  * gs1 = actionsAndStates(n)._1
  * gs2 = actionsAndStates(n-1)._1
  * actions = actionsAndStates(n)._2
  * then we have
  * gs2 = gs1(actions)
  *
  * This property is maintained in the addAction method.
  *
  * /!\ This implementation is currently not thread safe. A future implementation should come...
  */
final class ActionCollector(
    val originalGameState: GameState,
    val timeBetweenGameStates: Long = 2000,
    val timeToOldestGameState: Long = 60000
) {

  /**
    * actionsAndStates remembers the [[GameState]]s from the past and the actions from one to the other.
    * An element of the list is a couple GameState, List[GameAction] where the list is sorted from oldest to
    * newest, and are the actions between that GameState and the next one. The GameStates, however, are sorted
    * from newest to oldest.
    *
    * It means that if
    * gs1 = actionsAndStates(n)._1
    * gs2 = actionsAndStates(n-1)._1
    * actions = actionsAndStates(n)._2
    * then we have
    * gs2 = gs1(actions)
    *
    * This property is maintained in the addAction method.
    */
  private var actionsAndStates: List[(GameState, List[GameAction])] = List((originalGameState, Nil))

  /** This method is used in the tests. */
  def testActionAndStates: List[(GameState, List[GameAction])] = actionsAndStates

  def backupState(n: Int): (GameState, List[GameAction]) = actionsAndStates(n)

  /**
    * This is used by people trusting the server, typically players and actors.
    * Actions should first be removed before inserting these ones.
    */
  // TODO: add all actions that share the same time all together.
  def addActions(actions: List[GameAction]): Unit = {
    actions.foreach(addAction(_, needUpdate = false))
    updateGameState()
  }

  def shouldKeepAction(action: GameAction, state: GameState): Boolean = state.isLegalAction(action)

  /**
    * Adds all the actions from the list, and removes actions that turn out to be either not legal, or should no more
    * happen.
    *
    * @param actions The actions to add, ordered from oldest to newest
    * @return        The most ancient time at which actions were removed, and the list of the ids of actions that were
    *                removed
    */
  def addAndRemoveActions(actions: List[GameAction]): (Long, List[Long]) =
    if (actions.isEmpty) {
      (currentGameState.time, Nil)
    } else {
      val oldestTime: Long = actions.head.time

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
        actions,
        Nil
      ).foldLeft((gameStateUpTo(oldestTime), List[Long]())) {
          case ((state, toRemove), action) =>
            if (shouldKeepAction(action, state)) (action(state), toRemove)
            else (state, action.id +: toRemove)
        }
        ._2
        .reverse

      // Adding the new actions without updating the GameState.
      // This is because even new actions could have been immediately removed.
      actions.foreach(addAction(_, needUpdate = false))
      removeActions(oldestTime, actionIdsToRemove)

      (oldestTime, actionIdsToRemove)
    }

  /**
    * Remove all said actions from the list. You can opt for not updating the game state after this if you're going
    * to add actions after that.
    */
  def removeActions(oldestTime: Long, actionIds: List[Long], shouldUpdateGameState: Boolean = true): Unit = {

    val (after, before) = actionsAndStates.span(_._1.time >= oldestTime)

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
      .foldLeft((List[GameAction](), actionIds))({
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
      actionsAndStates = toRemain
    } else if (toRemain.nonEmpty) {
      actionsAndStates = (toRemain.head._1.applyActions(toRemain.head._2), remainingActionsAfter) +: toRemain
    } else {
      actionsAndStates = List((toBeChanged.last._1, remainingActionsAfter))
    }

    if (shouldUpdateGameState)
      updateGameState()
  }

  // TODO: when adding an action, we should check for the actions coming after to see if they are still legal.
  /**
    * Adds an action to the gameState.
    * When an action is added, it is put at the last possible position, after all actions at the same time that where
    * already added.
    *
    * @param action     the [[GameAction]] to add
    * @param needUpdate whether we should update the game state after inserting the action
    */
  def addAction(action: GameAction, needUpdate: Boolean = true): Unit =
    if (action.time > actionsAndStates.head._1.time + timeBetweenGameStates) {
      updateGameState()
      actionsAndStates = (currentGameState, List(action)) +: actionsAndStates
      if (timeToOldestGameState < actionsAndStates.head._1.time - actionsAndStates.last._1.time) {
        actionsAndStates = actionsAndStates.dropRight(1)
      }
      if (needUpdate) {
        updateGameState()
      }
    } else {
      def insertAction(action: GameAction, list: List[GameAction]): List[GameAction] = {
        // remaining is oldest to newest
        // treated   is newest to oldest
        @scala.annotation.tailrec
        def insertAcc(action: GameAction, treated: List[GameAction], remaining: List[GameAction]): List[GameAction] =
          if (remaining.isEmpty) (action +: treated).reverse
          else if (remaining.head.time > action.time) treated.reverse ++ (action +: remaining)
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
        actionsAndStates = add(action, actionsAndStates)
      } catch {
        case e: Throwable =>
          println("gs time " + actionsAndStates.head._1.time)
          println("action time " + action.time)
          throw e
      }

      if (needUpdate) {
        updateGameState()
      }
    }

  /**
    * Returns the GameState as it was at time time.
    */
  def gameStateUpTo(time: Long): GameState = actionsAndStates.find(_._1.time <= time) match {
    case Some((gs, actions)) =>
      gs.applyActions(actions.takeWhile(_.time <= time))
    case None =>
      println(("beuh", time))
      throw TooOldActionException(time.toString)
  }

  def actionsFrom(time: Long): List[GameAction] =
    actionsAndStates
      .take(actionsAndStates.indexWhere(_._1.time < time) + 1)
      .reverse
      .flatMap(_._2)
      .dropWhile(_.time <= time)

  private var _currentGameState: GameState = originalGameState

  private def updateGameState(): Unit =
    try {
      _currentGameState = actionsAndStates.head._1.applyActions(actionsAndStates.head._2)
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        throw new ActionCollector.FailedToUpdateGameStateException(actionsAndStates)
    }

  @inline def currentGameState: GameState = _currentGameState

  /**
    * Computes a GameState by adding some other actions to the stack.
    */
  @scala.annotation.tailrec
  def computeGameState(startingState: GameState, list1: List[GameAction], list2: List[GameAction]): GameState =
    if (list1.isEmpty)
      startingState.applyActions(list2)
    else if (list2.isEmpty)
      startingState.applyActions(list1)
    else if (list1.head.time < list2.head.time)
      computeGameState(list1.head(startingState), list1.tail, list2)
    else
      computeGameState(list2.head(startingState), list1, list2.tail)

}

object ActionCollector {

  final class FailedToUpdateGameStateException(val actionAndStates: List[(GameState, List[GameAction])])
      extends Exception(s"Failed to update game state")

}
