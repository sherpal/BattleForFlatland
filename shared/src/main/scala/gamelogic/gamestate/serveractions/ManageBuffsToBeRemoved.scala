package gamelogic.gamestate.serveractions

import gamelogic.buffs.Buff
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.{ActionGatherer, GameAction}
import gamelogic.utils.IdGeneratorContainer

final class ManageBuffsToBeRemoved extends ServerAction {

  private def removeBuffAction(actionId: GameAction.Id, buff: Buff): RemoveBuff =
    RemoveBuff(actionId, buff.appearanceTime + buff.duration, buff.bearerId, buff.buffId)

  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val removedBuffs = gameState.allBuffs
      .filter(_.isFinite)
      .filter(buff => startTime - buff.appearanceTime > buff.duration)
      .flatMap { buff =>
        removeBuffAction(genActionId(), buff) +: buff.endingAction(
          gameState,
          startTime
        )
      }
      .toVector

    val (nextCollector, oldestTime, idsToRemove) =
      currentState.masterAddAndRemoveActions(removedBuffs)

    (nextCollector, ServerAction.ServerActionOutput(removedBuffs, oldestTime, idsToRemove))
  }
}
