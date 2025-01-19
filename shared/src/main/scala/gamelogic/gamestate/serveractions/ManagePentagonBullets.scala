package gamelogic.gamestate.serveractions
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{ActionGatherer, GameAction}
import gamelogic.utils.IdGeneratorContainer

final class ManagePentagonBullets extends ServerAction {
  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {

    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val actions: Vector[GameAction] = gameState.pentagonBullets.flatMap { case (entityId, bullet) =>
      if (bullet.currentPosition(startTime) - bullet.pos).modulus > bullet.range then {
        RemoveEntity(genActionId(), startTime, entityId) :: Nil
      } else
        bullet.collideEnemy(gameState, startTime) match {
          case Some(enemy) =>
            EntityTakesDamage(
              genActionId(),
              startTime,
              enemy.id,
              bullet.damage,
              bullet.ownerId
            ) :: RemoveEntity(
              genActionId(),
              startTime,
              entityId
            ) :: Nil
          case None => Nil
        }
    }.toVector

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }
}
