package application.ai.boss

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.Ability.UseId
import gamelogic.abilities.boss.boss105.{Resonance, Reunion, Skewer, VolatileMark}
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.{EntityStartsCasting, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

/** The brain of [[Boss105]].
  *
  * It chases its target (the player with the biggest threat), and uses its abilities by decreasing
  * order of priority. It knows nothing about its Shade: the dance of the twins is made of buffs and
  * of what the players do.
  */
object Boss105Controller extends AIController[Boss105, SpawnBoss] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Boss105,
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
          Boss105.meleeRange,
          Boss105.fullSpeed,
          Boss105.fullSpeed / 4,
          me.speed,
          me.moving,
          me.rotation,
          obstacleGraph
        )

        def maybeCast(ability: Ability): Option[EntityStartsCasting] =
          Some(ability)
            .filter(me.canUseAbilityBoolean(_, startTime, currentGameState))
            .filter(_.canBeCastBoolean(currentGameState, startTime))
            .map(_.toStartCasting(startTime))

        // anyone but the one the boss is hitting, if possible
        def maybeSkewer = {
          val players    = Random.shuffle(currentGameState.players.values.toVector)
          val candidates = players.filterNot(_.id == target.id)
          candidates.headOption
            .orElse(players.headOption)
            .flatMap(victim => maybeCast(Skewer(UseId.dummy, startTime, me.id, victim.id)))
        }

        useAbility(
          Vector(
            maybeCast(Reunion(UseId.dummy, startTime, me.id)),
            maybeSkewer,
            maybeCast(VolatileMark(UseId.dummy, startTime, me.id)),
            maybeCast(Resonance(UseId.dummy, startTime, me.id)),
            me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))
          ),
          maybeChangeTarget,
          maybeMove
        )
      }
      .getOrElse(Vector.empty)

  override protected def getMe(gameState: GameState, entityId: Id): Option[Boss105] =
    gameState.bosses.get(entityId).collect { case boss: Boss105 => boss }

}
