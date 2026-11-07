package application.ai.boss

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability.UseId
import gamelogic.abilities.boss.boss110.{
  ExplodeBombs,
  PlaceBombPods,
  SpawnBigGuies,
  SpawnSmallGuies
}
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** The brain of [[Boss110]].
  *
  * Registered in `AIManager` on the `SpawnBoss` action carrying Boss110's name. It walks to its
  * target and casts its abilities as soon as they are available. All the randomness (where the
  * bombs go) lives in the abilities themselves.
  */
object Boss110Controller extends AIController[Boss110, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Boss110,
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
        val maybeChangeTarget = changeTarget(me, target.id, startTime)

        val maybeMove = aiMovementToTargetWithGraph(
          me.id,
          startTime,
          timeSinceLastFrame,
          currentPosition,
          me.shape.radius,
          target.currentPosition(startTime + 100),
          Boss110.meleeRange,
          Boss110.fullSpeed,
          Boss110.fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph
        )

        def maybeCast(ability: gamelogic.abilities.Ability) =
          maybeAbilityUsage(me, ability, currentGameState).map(_.toStartCasting(startTime))

        // `useAbility` picks the first defined candidate. The explosion comes first: it is only
        // castable while bombs are down, and players are waiting on their bomb for it.
        useAbility(
          Vector(
            maybeCast(ExplodeBombs(UseId.dummy, startTime, me.id)),
            maybeCast(PlaceBombPods(UseId.dummy, startTime, me.id)),
            maybeCast(SpawnBigGuies(UseId.dummy, startTime, me.id)),
            maybeCast(SpawnSmallGuies(UseId.dummy, startTime, me.id)),
            me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Vector.empty)

  override protected def getMe(gameState: GameState, entityId: Id): Option[Boss110] =
    gameState.bosses.get(entityId).collect { case boss110: Boss110 => boss110 }

}
