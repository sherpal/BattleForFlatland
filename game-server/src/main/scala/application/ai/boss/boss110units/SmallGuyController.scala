package application.ai.boss.boss110units

import application.ai.AIController
import application.ai.utils.*
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss110.SmallGuy
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.boss110.AddSmallGuy
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** The brain of a Boss110 [[SmallGuy]]: it runs to its target and hits it. Where it goes is what
  * matters, since the creeping shadow follows the small guys around.
  *
  * Registered in `AIManager` on the [[AddSmallGuy]] action (one instance per add).
  */
class SmallGuyController extends AIController[SmallGuy, AddSmallGuy] {

  override protected def takeActions(
      currentGameState: GameState,
      me: SmallGuy,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): Vector[GameAction] = maybeTarget.fold(Vector.empty[GameAction]) { target =>
    val maybeChangeTarget = changeTarget(me, target.id, startTime)

    val maybeMove = aiMovementToTarget(
      me.id,
      startTime,
      timeSinceLastFrame,
      currentPosition,
      me.shape.radius,
      target.currentPosition(startTime),
      SmallGuy.range,
      SmallGuy.fullSpeed,
      SmallGuy.fullSpeed / 10,
      me.speed,
      me.moving,
      me.rotation
    )

    val maybeAttack = me
      .maybeAutoAttack(startTime, currentGameState)
      .filterNot(_ => currentGameState.entityIsCasting(me.id))
      .map(_.toStartCasting(startTime))

    Vector(maybeChangeTarget, maybeMove, maybeAttack).flatten
  }

  override protected def getMe(gameState: GameState, entityId: Id): Option[SmallGuy] =
    gameState.entityByIdAs[SmallGuy](entityId)
}
