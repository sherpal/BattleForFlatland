package gamelogic.gamestate.serveractions
import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.EntityCastingInterrupted
import gamelogic.utils.IdGeneratorContainer

/**
  * Entities that are casting, but moving, should stop casting.
  */
final class ManageStopCastingMovements extends ServerAction {
  def apply(
      currentState: ActionGatherer,
      nowGenerator: () => Long
  )(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val entityStoppedCastingActions = gameState.castingEntityInfo
      .map { case (id, info) => (id, gameState.withAbilityEntitiesById(id), info.positionWhenStarted) }
      .flatMap {
        case (id, None, _)                                                    => Some(id)
        case (id, Some(withAbility), position) if position != withAbility.pos => Some(id)
        case _                                                                => None
      }
      .map(id => EntityCastingInterrupted(idGeneratorContainer.gameActionIdGenerator(), startTime, id))
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(entityStoppedCastingActions)

    (nextCollector, ServerAction.ServerActionOutput(entityStoppedCastingActions, oldestTime, idsToRemove))
  }
}
