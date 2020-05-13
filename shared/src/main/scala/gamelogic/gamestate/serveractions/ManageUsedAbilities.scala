package gamelogic.gamestate.serveractions

import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.UseAbility
import gamelogic.utils.{AbilityUseIdGenerator, BuffIdGenerator, EntityIdGenerator, GameActionIdGenerator}

final class ManageUsedAbilities extends ServerAction {
  def apply(
      currentState: ImmutableActionCollector,
      nowGenerator: () => Long
  )(
      implicit gameActionIdGenerator: GameActionIdGenerator,
      entityIdGenerator: EntityIdGenerator,
      abilityUseIdGenerator: AbilityUseIdGenerator,
      buffIdGenerator: BuffIdGenerator
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val usedAbilities = gameState.castingEntityInfo.valuesIterator
      .filter(castingInfo => startTime - castingInfo.startedTime >= castingInfo.ability.castingTime)
      .map(
        castingInfo =>
          UseAbility(
            gameActionIdGenerator(),
            startTime,
            castingInfo.casterId,
            abilityUseIdGenerator(),
            castingInfo.ability.copyWithNewTimeAndId(startTime, abilityUseIdGenerator())
          )
      )
      .flatMap(usage => usage :: usage.ability.createActions(gameState))
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(usedAbilities)

    (nextCollector, ServerAction.ServerActionOutput(usedAbilities, oldestTime, idsToRemove))
  }
}
