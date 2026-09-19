package application.ai.boss.boss104units

import application.ai.AIController
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss104.BigGuy
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.boss104.AddBigGuy
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.BigGuyKick
import gamelogic.gamestate.gameactions.EntityStartsCasting

/** The brain of a [[BigGuy]] add.
  *
  * Registered in `AIManager` on the [[AddBigGuy]] action. Note that, unlike the boss, it is a class
  * and not an object: there is one instance per spawned add.
  *
  * It uses `aiMovementToTarget` (straight line) rather than `aiMovementToTargetWithGraph`, since it
  * does not need to path around obstacles. Beware though that `AIController` still requires a
  * pathfinding graph for its collision radius: `BigGuy.shape.radius` must appear in the list of
  * radii given to the `GraphManager` in `AIManager`, otherwise this controller never runs.
  */
class BigGuyController extends AIController[BigGuy, AddBigGuy] {

  override protected def takeActions(
      currentGameState: GameState,
      me: BigGuy,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] = maybeTarget
    .filterNot(_ => currentGameState.entityIsCasting(me.id))
    .fold(Vector.empty[GameAction]) { target =>
      val maybeChangeTarget = changeTarget(me, target.id, startTime)

      val maybeMove = aiMovementToTarget(
        me.id,
        startTime,
        timeSinceLastFrame,
        currentPosition,
        me.shape.radius,
        target.currentPosition(startTime),
        BigGuy.range,
        BigGuy.fullSpeed,
        BigGuy.fullSpeed / 10,
        me.speed,
        me.moving,
        me.rotation
      )

      val maybeAttack =
        me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))

      val maybeKick = application.ai.utils
        .maybeAbilityUsage(
          me,
          BigGuyKick(Ability.UseId.dummy, startTime, me.id, target.id),
          currentGameState
        )
        .map(_.toStartCasting(startTime))

      Vector(maybeChangeTarget, maybeKick, maybeMove, maybeAttack).flatten
    }

  override protected def getMe(gameState: GameState, entityId: Id): Option[BigGuy] =
    gameState.entityByIdAs[BigGuy](entityId)
}
