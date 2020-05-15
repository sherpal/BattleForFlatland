package gamelogic.gamestate.serveractions

import gamelogic.buffs.{Buff, PassiveBuff, TickerBuff}
import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.RemoveBuff
import gamelogic.utils.{AbilityUseIdGenerator, BuffIdGenerator, EntityIdGenerator, GameActionIdGenerator}

final class ManageBuffsToBeRemoved extends ServerAction {

  private def removeBuffAction(buff: Buff): RemoveBuff =
    RemoveBuff(0L, buff.appearanceTime + buff.duration, buff.bearerId, buff.buffId)

  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit gameActionIdGenerator: GameActionIdGenerator,
      entityIdGenerator: EntityIdGenerator,
      abilityUseIdGenerator: AbilityUseIdGenerator,
      buffIdGenerator: BuffIdGenerator
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val removedBuffs = gameState.allBuffs
      .filter(buff => startTime - buff.appearanceTime > buff.duration)
      .flatMap {
        case buff: PassiveBuff => removeBuffAction(buff) :: buff.endingAction(gameState)
        case buff: TickerBuff  => removeBuffAction(buff) :: Nil
      }
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(removedBuffs)

    (nextCollector, ServerAction.ServerActionOutput(removedBuffs, oldestTime, idsToRemove))
  }
}
