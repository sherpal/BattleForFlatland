package application.masterbehaviours

import gamelogic.gamestate.ActionGatherer
import application.*
import gamelogic.gamestate.serveractions.ServerAction
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.AddAndRemoveActions
import concurrent.*

trait GameMasterBehaviour(using val idGeneratorContainer: IdGeneratorContainer) {

  def loop(
      startTime: Long,
      pendingActions: Vector[GameAction],
      actionGatherer: ActionGatherer
  ): (ServerAction.ServerActionOutput, ActionGatherer)

  inline def now: Long = System.currentTimeMillis()

}
