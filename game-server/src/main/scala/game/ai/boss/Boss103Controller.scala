package game.ai.boss

import game.ai.utils._
import gamelogic.abilities.boss.boss102.{PutDamageZones, PutLivingDamageZoneOnTarget, SpawnHound}
import gamelogic.abilities.boss.boss103.CleansingNova
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.dawnoftime.{Boss102, Boss103}
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

object Boss103Controller extends AIController[Boss103, SpawnBoss] {
  protected def takeActions(
      currentGameState: GameState,
      me: Boss103,
      currentPosition: Complex,
      startTime: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): List[GameAction] =
    Option
      .unless(currentGameState.entityIsCasting(me.id))(maybeTarget)
      .flatten
      .map { target =>
        // If the boss is casting, he doesn't do anything else.
        // If the boss has no target, the only possibility is that all players are dead.
        // In that case, the game either has not started yet or it will end very soon so we don't do anything.

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime),
          Boss102.meleeRange,
          Boss102.fullSpeed,
          Boss102.fullSpeed / 4,
          me.moving,
          me.rotation,
          obstacleGraph,
          position => !currentGameState.obstacles.valuesIterator.exists(_.collidesShape(me.shape, position, 0, 0))
        )

        val maybeUseCleansingNova =
          Some(CleansingNova(0L, startTime, me.id))
            .filter(me.canUseAbility(_, startTime))
            .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))

        useAbility(
          List(
            maybeUseCleansingNova,
            me.maybeAutoAttack(startTime, currentGameState)
              .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Nil)

  protected def getMe(gameState: GameState, entityId: Id): Option[Boss103] =
    gameState.bosses.get(entityId).collect { case boss103: Boss103 => boss103 }
}
