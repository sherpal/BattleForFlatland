package gamelogic.gamestate.serveractions
import gamelogic.buffs.abilities.classes.{Facets, Fractured}
import gamelogic.gamestate.gameactions.{EntityTakesDamage, RemoveEntity}
import gamelogic.gamestate.{ActionGatherer, GameAction}
import gamelogic.physics.PosInt
import gamelogic.utils.IdGeneratorContainer

final class ManagePentagonBullets extends ServerAction {
  def apply(currentState: ActionGatherer, nowGenerator: () => Long)(using
      IdGeneratorContainer
  ): (ActionGatherer, ServerAction.ServerActionOutput) = {

    val startTime = nowGenerator()
    val gameState = currentState.currentGameState

    val (outOfRange, flying) = gameState.pentagonBullets.toVector.partition { (_, bullet) =>
      (bullet.currentPosition(startTime) - bullet.pos).modulus > bullet.range
    }

    val hits = flying.flatMap((entityId, bullet) =>
      bullet.collideEnemy(gameState, startTime).map(enemy => (entityId, bullet, enemy))
    )

    val hitActions = hits.flatMap { (entityId, bullet, enemy) =>
      Vector(
        EntityTakesDamage(
          genActionId(),
          startTime,
          enemy.id,
          bullet.damage,
          bullet.ownerId,
          bullet.abilityId
        ),
        RemoveEntity(genActionId(), startTime, entityId)
      )
    }

    val fracturesActions = hits
      .collect { case (_, bullet, enemy) if bullet.fractures => (enemy.id, bullet.ownerId) }
      .distinct
      .flatMap((enemyId, ownerId) => Fractured.refresh(gameState, enemyId, ownerId, startTime))

    // All the facets lit by the bullets of a Pentagon at once, so that none is lost when several
    // of its bullets hit together
    val facetsActions = hits
      .collect { case (_, bullet, _) if !bullet.fractures => bullet.ownerId }
      .groupMapReduce(identity)(_ => 1)(_ + _)
      .toVector
      .filter((ownerId, _) => gameState.players.isDefinedAt(ownerId))
      .flatMap { (ownerId, lit) =>
        val stacks = Facets.stacksOn(gameState, ownerId).fold(0)(_.toInt) + lit
        Facets.setStacks(gameState, ownerId, startTime, PosInt.maybe(stacks))
      }

    val actions: Vector[GameAction] =
      outOfRange.map((entityId, _) => RemoveEntity(genActionId(), startTime, entityId)) ++
        hitActions ++ fracturesActions ++ facetsActions

    val (nextCollector, oldestTime, idsToRemove) = currentState.masterAddAndRemoveActions(actions)

    (nextCollector, ServerAction.ServerActionOutput(actions, oldestTime, idsToRemove))
  }
}
