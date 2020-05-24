package gamelogic.gamestate.serveractions

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
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
      .filter(buff => startTime - buff.appearanceTime > buff.duration)
      .flatMap {
        case buff: PassiveBuff =>
          removeBuffAction(idGeneratorContainer.gameActionIdGenerator(), buff) :: buff.endingAction(gameState)
        case buff: TickerBuff => removeBuffAction(idGeneratorContainer.gameActionIdGenerator(), buff) :: Nil
      }
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(removedBuffs)

    (nextCollector, ServerAction.ServerActionOutput(removedBuffs, oldestTime, idsToRemove))
  }
}
