package gamelogic.gamestate.serveractions

import gamelogic.entities.Entity
import gamelogic.gamestate.ImmutableActionCollector
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.utils.IdGeneratorContainer

final class ManageDeadAIs extends ServerAction {
  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {
    val now = nowGenerator()

    val actions = currentState.currentGameState.allLivingEntities
      .filter(_.teamId == Entity.teams.mobTeam)
      .filter(_.life <= 0)
      .map(
        entity =>
          RemoveEntity(
            idGeneratorContainer.gameActionIdGenerator(),
            now,
            entity.id
          )
      )
      .toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }

}
