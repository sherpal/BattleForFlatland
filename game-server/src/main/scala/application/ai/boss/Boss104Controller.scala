package application.ai.boss

import application.ai.utils.*

import application.ai.AIController
import gamelogic.entities.boss.dawnoftime.Boss104
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.entities.classes.PlayerClass
import gamelogic.physics.pathfinding.Graph
import gamelogic.entities.Entity.Id
import gamelogic.gamestate.GameAction
import gamelogic.physics.Complex
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.gamestate.gameactions.EntityStartsCasting

object Boss104Controller extends AIController[Boss104, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Boss104,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)
      .flatten
      .map { target =>
        // If the boss is casting, he doesn't do anything else.
        // If the boss has no target, the only possibility is that all players are dead.
        // In that case, the game either has not started yet or it will end very soon so we don't do anything.

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val elapsedSinceLastFrame = startTime - timeSinceLastFrame

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          elapsedSinceLastFrame,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime + 100),
          Boss104.meleeRange,
          Boss102.fullSpeed,
          Boss102.fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph,
          position =>
            !currentGameState.obstacles.valuesIterator
              .exists(_.collidesShape(me.shape, position, 0, 0))
        )

        useAbility(
          Vector(
            me.maybeAutoAttack(startTime, currentGameState)
              .map(ability =>
                EntityStartsCasting(GameAction.Id.dummy, startTime, ability.castingTime, ability)
              )
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Vector.empty)

  override protected def getMe(gameState: GameState, entityId: Id): Option[Boss104] =
    gameState.bosses.get(entityId).collect { case boss104: Boss104 => boss104 }

}
