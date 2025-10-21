package gamelogic.gamestate.serveractions

import gamelogic.entities.Entity
import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.gameactions.RemoveEntity
import gamelogic.utils.IdGeneratorContainer

final class ManageDeadAIs extends ServerAction {
  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {
    val now = nowGenerator()

    val actions = (for {
      entity <- currentState.currentGameState.allLivingEntities
      if entity.teamId == Entity.teams.mobTeam
      if entity.life <= 0
      remove = RemoveEntity(genActionId(), now, entity.id)
      buffsRemovedActions = for {
        buff   <- currentState.currentGameState.allBuffsOfEntity(entity.id)
        action <- buff.bearerDiedAction(currentState.currentGameState, nowGenerator())
      } yield action
    } yield Iterator(remove) ++ buffsRemovedActions).flatten.toVector

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }

}
