package application.ai.boss.boss105units

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss105.Siphon
import gamelogic.buffs.boss.boss105.Yearning
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.boss105.AddShade
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.util.Random

/** The brain of the [[Shade]], registered in `AIManager` on the [[AddShade]] action.
  *
  * It walks straight to its target (the player with the biggest threat on it), hits it, and siphons
  * somebody else every time it can. Like all the controllers of adds, there is one instance per
  * Shade.
  */
class ShadeController extends AIController[Shade, AddShade] {

  /** When the boss calls it, the Shade ignores everybody and runs to the boss. */
  private def yearningMovement(
      gameState: GameState,
      me: Shade,
      currentPosition: Complex,
      time: Long,
      timeSinceLastFrame: Long
  ): Option[Vector[GameAction]] =
    Option
      .when(gameState.hasBuffOfType[Yearning](me.id))(
        gameState.bosses.values.collectFirst { case boss: Boss105 => boss }
      )
      .map { maybeBoss =>
        maybeBoss.fold(Vector.empty[GameAction]) { boss =>
          aiMovementToTarget(
            me.id,
            time,
            timeSinceLastFrame,
            currentPosition,
            me.shape.radius,
            boss.currentPosition(time),
            boss.shape.radius + me.shape.radius,
            Shade.yearningSpeed,
            Shade.yearningSpeed / 10,
            me.speed,
            me.moving,
            me.rotation
          ).toVector
        }
      }

  override protected def takeActions(
      currentGameState: GameState,
      me: Shade,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] = maybeTarget
    .filterNot(_ => currentGameState.entityIsCasting(me.id))
    .fold(Vector.empty[GameAction]) { target =>
      yearningMovement(currentGameState, me, currentPosition, startTime, timeSinceLastFrame)
        .getOrElse(
          followTarget(currentGameState, me, currentPosition, startTime, timeSinceLastFrame, target)
        )
    }

  private def followTarget(
      currentGameState: GameState,
      me: Shade,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      target: PlayerClass
  ): Vector[GameAction] = {
    val maybeChangeTarget = changeTarget(me, target.id, startTime)

    val maybeMove = aiMovementToTarget(
      me.id,
      startTime,
      timeSinceLastFrame,
      currentPosition,
      me.shape.radius,
      target.currentPosition(startTime),
      Shade.meleeRange,
      Shade.fullSpeed,
      Shade.fullSpeed / 10,
      me.speed,
      me.moving,
      me.rotation
    )

    // somebody other than its target, if possible
    val maybeSiphon = Random
      .shuffle(currentGameState.players.values.toVector)
      .sortBy(_.id == target.id)
      .iterator
      .flatMap(victim =>
        maybeAbilityUsage(
          me,
          Siphon(Ability.UseId.dummy, startTime, me.id, victim.id),
          currentGameState
        )
      )
      .nextOption()
      .map(_.toStartCasting(startTime))

    val maybeAttack =
      me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))

    maybeSiphon match {
      case Some(siphon) =>
        Vector(
          maybeChangeTarget,
          stopMoving(startTime, me.id, me.moving, currentPosition, me.speed, me.rotation),
          Some(siphon)
        ).flatten
      case None => Vector(maybeChangeTarget, maybeMove, maybeAttack).flatten
    }
  }

  override protected def getMe(gameState: GameState, entityId: Id): Option[Shade] =
    gameState.entityByIdAs[Shade](entityId)
}
