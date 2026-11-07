package application.ai.boss.boss110units

import application.ai.AIController
import application.ai.utils.*
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss110.addsabilities.PutBrokenArmor
import gamelogic.entities.Entity.Id
import gamelogic.entities.boss.boss110.BigGuy
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.boss110.AddBigGuy
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** The brain of a Boss110 [[BigGuy]]: it runs to its target, and breaks its armour as often as it
  * can.
  *
  * Registered in `AIManager` on the [[AddBigGuy]] action (one instance per add).
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

      val maybePutBrokenArmor =
        maybeAbilityUsage(
          me,
          PutBrokenArmor(Ability.UseId.dummy, startTime, me.id, target.id),
          currentGameState
        ).map(_.toStartCasting(startTime))

      val maybeAttack =
        me.maybeAutoAttack(startTime, currentGameState).map(_.toStartCasting(startTime))

      useAbility(Vector(maybePutBrokenArmor, maybeAttack), maybeChangeTarget, maybeMove)
    }

  override protected def getMe(gameState: GameState, entityId: Id): Option[BigGuy] =
    gameState.entityByIdAs[BigGuy](entityId)
}
