package gamelogic.gamestate.serveractions
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{GameAction, ImmutableActionCollector}
import gamelogic.utils.IdGeneratorContainer

final class ManagePentagonBullets extends ServerAction {
  def apply(currentState: ImmutableActionCollector, nowGenerator: () => Long)(
      implicit idGeneratorContainer: IdGeneratorContainer
  ): (ImmutableActionCollector, ServerAction.ServerActionOutput) = {

    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val actions: List[GameAction] = gameState.pentagonBullets.flatMap {
      case (entityId, bullet) =>
        if ((bullet.currentPosition(startTime) - bullet.pos).modulus > bullet.range) {
          RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), startTime, entityId) :: Nil
        } else
          bullet.collideEnemy(gameState, startTime) match {
            case Some(enemy) =>
              EntityTakesDamage(
                idGeneratorContainer.gameActionIdGenerator(),
                startTime,
                enemy.id,
                bullet.damage,
                bullet.ownerId
              ) :: RemoveEntity(idGeneratorContainer.gameActionIdGenerator(), startTime, entityId) :: Nil
            case None => Nil
          }
    }.toList

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }
}
