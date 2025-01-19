package application.masterbehaviours
import gamelogic.gamestate.ActionGatherer
import application.*
import gamelogic.gamestate.serveractions.ServerAction
import gamelogic.utils.IdGeneratorContainer
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.AddAndRemoveActions
import gamelogic.gamestate.serveractions.ServerAction.ServerActionOutput
import concurrent.*
import gamelogic.gamestate.serveractions.*
import gamelogic.utils.GameActionIdGenerator

class InGameBehaviour(using IdGeneratorContainer) extends GameMasterBehaviour {

  private val serverAction = ManageUsedAbilities() ++
    ManageStopCastingMovements() ++
    ManageTickerBuffs() ++
    ManageBuffsToBeRemoved() ++
    ManageDeadPlayers() ++
    ManageEndOfGame() ++
    ManagePentagonBullets() ++
    ManageDeadAIs()

  override def loop(
      startTime: Long,
      pendingActions: Vector[GameAction],
      actionGatherer: ActionGatherer
  ): (ServerActionOutput, ActionGatherer) = {
    val sortedActions = pendingActions.sorted
      .map(_.changeIdWithGen())
      .toVector

    try {

      /** First adding actions from entities */
      val (nextCollector, oldestTimeToRemove, idsToRemove) =
        actionGatherer.masterAddAndRemoveActions(sortedActions)

      /** Making all the server specific checks */
      val (finalCollector, output) = serverAction(
        nextCollector,
        () => System.currentTimeMillis
      )

      /** Sending outcome back to entities. */
      val finalOutput = ServerAction
        .ServerActionOutput(
          sortedActions,
          oldestTimeToRemove,
          idsToRemove
        )
        .merge(output)
      (finalOutput, finalCollector)
    } catch {
      case e: Throwable =>
        println(e.getStackTrace.toList.mkString("\n"))
        e.printStackTrace()
        throw e
    }
  }
}
