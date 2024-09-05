package application.masterbehaviours

import gamelogic.gamestate.ActionGatherer
import application.*
import gamelogic.gamestate.serveractions.ServerAction
import models.bff.ingame.InGameWSProtocol.AddAndRemoveActions
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.gameactions.UpdateTimestamp
import concurrent.*

class PreGameBehaviour(using IdGeneratorContainer) extends GameMasterBehaviour {

  def loop(
      startTime: Long,
      pendingActions: Vector[GameAction],
      actionGatherer: ActionGatherer
  ): (ServerAction.ServerActionOutput, ActionGatherer) = {

    /** Adding a [[gamelogic.gamestate.gameactions.UpdateTimestamp]] so that there are actions even
      * if no body does anything. (Otherwise the game can crash at launch)
      */
    val sortedActions = (UpdateTimestamp(0L, now) +: pendingActions).sorted
      .map(_.changeId(idGeneratorContainer.gameActionIdGenerator()))
      .toVector

    val (nextCollector, oldestTimeToRemove, idsToRemove) =
      actionGatherer.masterAddAndRemoveActions(sortedActions)

    val output = ServerAction.ServerActionOutput(
      sortedActions,
      oldestTimeToRemove,
      idsToRemove
    )

    (output, nextCollector)
  }

}
