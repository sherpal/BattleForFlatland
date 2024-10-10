package application.ai.goodais.bosses.boss102

import gamelogic.entities.Entity
import application.ai.goodais.classes.PentagonAIController
import gamelogic.entities.classes.Pentagon
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.gameactions.MovingBodyMoves

final case class PentagonForBoss102(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index)
    with Boss102GoodAIController[Pentagon] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Pentagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = findSafeSpot(currentGameState, startTime) match {
    case None =>
      val maybeBoss = currentGameState.bosses.values.headOption
      val hounds    = houndsNow(currentGameState)

      def maybePentagonZone = Option
        .when(hounds.length >= 4) {
          val targetPos = medianOfEntities(hounds, startTime)
          maybePentagonZoneUsage(currentGameState, startTime, me, targetPos)
        }
        .flatten

      def maybePentagonBullet = maybeBoss.flatMap(boss =>
        maybePentagonBulletUsage(
          currentGameState,
          startTime,
          me,
          (boss.currentPosition(startTime) - currentPosition).arg
        )
      )

      val maybeStopMoving = stopMoving(startTime, me, currentPosition, me.rotation)

      Vector(
        maybePentagonZone.orElse(maybePentagonBullet),
        maybeStopMoving
      ).flatten
    case Some(pos) =>
      val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)
      preGameMovement(startTime, me, currentPosition, pos, travelledDistance)
  }

}
