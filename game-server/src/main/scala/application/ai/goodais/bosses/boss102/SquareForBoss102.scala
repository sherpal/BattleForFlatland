package application.ai.goodais.bosses.boss102

import gamelogic.entities.Entity
import application.ai.goodais.classes.SquareAIController
import gamelogic.entities.classes.Square
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.entities.boss.boss102.BossHound

final case class SquareForBoss102(index: Int, entityId: Entity.Id)
    extends SquareAIController(index)
    with Boss102GoodAIController[Square] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Square,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = findSafeSpot(currentGameState, startTime) match {
    case None =>
      val maybeBoss = currentGameState.bosses.values.headOption
      val hounds    = houndsNow(currentGameState)

      def maybeTaunt =
        for {
          // finding small guy not targetting me, or for which I have no threat
          target <- hounds
            .find(_.targetId != me.id)
            .orElse(hounds.find(_.damageThreats.getOrElse(me.id, 0.0) == 0.0))
          taunt <- maybeTauntUsage(currentGameState, startTime, me, target)
        } yield taunt

      def maybeTauntBoss = maybeBoss.flatMap(boss =>
        Option
          .unless(isMyThreatTowardsTargetEnough(boss, me))(
            maybeTauntUsage(currentGameState, startTime, me, boss)
          )
          .flatten
      )

      def maybeCleave = Option
        .when(hounds.length >= 3) {
          val targetPos = medianOfEntities(hounds, startTime)
          val direction = (targetPos - me.pos).arg
          maybeCleaveUsage(currentGameState, startTime, me, direction)
        }
        .flatten

      val maybeStopMoving = stopMoving(startTime, me, currentPosition, me.rotation)

      Vector(
        maybeStopMoving,
        maybeTaunt.orElse(maybeTauntBoss).orElse(maybeCleave)
      ).flatten
    case Some(pos) =>
      val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)
      preGameMovement(startTime, me, currentPosition, pos, travelledDistance)
  }

}
