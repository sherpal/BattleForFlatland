package gamelogic.gamestate.serveractions

import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.utils.IdGeneratorContainer

final class ManageUsedAbilities extends ServerAction {
  def apply(
      currentState: ActionGatherer,
      nowGenerator: () => Long
  )(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val usedAbilities = gameState.castingEntityInfo.valuesIterator
      .filter(castingInfo => startTime - castingInfo.startedTime >= castingInfo.ability.castingTime)
      .map(
        castingInfo =>
          UseAbility(
            idGeneratorContainer.gameActionIdGenerator(),
            startTime,
            castingInfo.casterId,
            idGeneratorContainer.abilityUseIdGenerator(),
            castingInfo.ability.copyWithNewTimeAndId(startTime, idGeneratorContainer.abilityUseIdGenerator())
          )
      )
      .flatMap(usage => usage :: usage.ability.createActions(gameState))
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(usedAbilities)

    (nextCollector, ServerAction.ServerActionOutput(usedAbilities, oldestTime, idsToRemove))
  }
}
