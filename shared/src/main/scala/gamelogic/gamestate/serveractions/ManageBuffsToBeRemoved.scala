package gamelogic.gamestate.serveractions

import gamelogic.buffs.Buff
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.gamestate.{GameAction, ImmutableActionCollector}
import gamelogic.utils.IdGeneratorContainer

final class ManageBuffsToBeRemoved extends ServerAction {

  private def removeBuffAction(actionId: GameAction.Id, buff: Buff): RemoveBuff =
    RemoveBuff(actionId, buff.appearanceTime + buff.duration, buff.bearerId, buff.buffId)

  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val removedBuffs = gameState.allBuffs
      .filter(_.isFinite)
      .filter(buff => startTime - buff.appearanceTime > buff.duration)
      .flatMap { buff =>
        removeBuffAction(idGeneratorContainer.gameActionIdGenerator(), buff) :: buff.endingAction(gameState, startTime)
      }
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(removedBuffs)

    (nextCollector, ServerAction.ServerActionOutput(removedBuffs, oldestTime, idsToRemove))
  }
}
