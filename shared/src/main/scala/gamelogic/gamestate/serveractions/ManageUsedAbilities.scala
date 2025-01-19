package gamelogic.gamestate.serveractions

import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.utils.IdGeneratorContainer

final class ManageUsedAbilities extends ServerAction {
  def apply(
      currentState: ActionGatherer,
      nowGenerator: () => Long
  )(using
      idGeneratorContainer: IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val usedAbilities = gameState.castingEntityInfo.valuesIterator
      .filter(castingInfo => startTime - castingInfo.startedTime >= castingInfo.ability.castingTime)
      .map(castingInfo =>
        UseAbility(
          idGeneratorContainer.actionId(),
          startTime,
          castingInfo.casterId,
          idGeneratorContainer.abilityUseId(),
          castingInfo.ability
            .copyWithNewTimeAndId(startTime, idGeneratorContainer.abilityUseId())
        )
      )
      .flatMap(usage => usage +: usage.ability.createActions(gameState))
      .toVector

    val (nextCollector, oldestTime, idsToRemove) =
      currentState.masterAddAndRemoveActions(usedAbilities)

    (nextCollector, ServerAction.ServerActionOutput(usedAbilities, oldestTime, idsToRemove))
  }
}
