package game.ai.boss
import game.ai.utils._
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random
import gamelogic.abilities.boss.boss110.SpawnBigGuies
import gamelogic.abilities.boss.boss110.PlaceBombPods
import gamelogic.entities.boss.boss110.BombPod
import gamelogic.abilities.boss.boss110.ExplodeBombs
import gamelogic.abilities.Ability

object Boss110Controller extends AIController[Boss110, SpawnBoss] {
  protected def takeActions(
      currentGameState: GameState,
      me: Boss110,
      currentPosition: Complex,
      startTime: Long,
      lastTimeStamp: Long,
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

        def iMightCastThis[T <: Ability](ability: T) = maybeAbilityUsage(me, ability, currentGameState)

        /** changing target */
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val elapsedSinceLastFrame = startTime - lastTimeStamp

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          elapsedSinceLastFrame,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime + 100),
          Boss110.meleeRange,
          Boss110.fullSpeed,
          Boss110.fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph,
          position => !currentGameState.obstacles.valuesIterator.exists(_.collidesShape(me.shape, position, 0, 0))
        )

        val maybeSpawnBigGuies = Some(SpawnBigGuies(0L, startTime, me.id))
          .filter(me.canUseAbilityBoolean(_, startTime))
          .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))

        val maybePlaceBombPods = iMightCastThis(
          PlaceBombPods(0L, startTime, me.id, Nil) // Nil to not compute for nothing
        ).map(
            _.copy(
              positions = PlaceBombPods
                .randomPositionsInSquare(-Boss110.halfWidth / 2, Boss110.halfHeight / 2, PlaceBombPods.numberOfBombs)
            )
          )
          .startCasting

        val maybeExplodeBombs = iMightCastThis(ExplodeBombs(0L, startTime, me.id)).startCasting

        useAbility(
          List(
            //maybeSpawnBigGuies,
            maybePlaceBombPods,
            maybeExplodeBombs,
            me.maybeAutoAttack(startTime, currentGameState)
              .map(ability => EntityStartsCasting(0L, startTime, ability.castingTime, ability))
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Nil)

  protected def getMe(gameState: GameState, entityId: Id): Option[Boss110] =
    gameState.bosses.get(entityId).collect { case boss110: Boss110 => boss110 }
}
