package application.ai.goodais.bosses.boss102

import gamelogic.entities.Entity
import application.ai.goodais.classes.TriangleAIController
import gamelogic.entities.classes.Triangle
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.abilities.WithTargetAbility

final case class TriangleForBoss102(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index)
    with Boss102GoodAIController[Triangle] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Triangle,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = findSafeSpot(currentGameState, startTime) match {
    case None =>
      val maybeBoss = currentGameState.bosses.values.headOption

      def iHaveLinvingZoneDebuff = currentGameState.hasBuffOfType[LivingDamageZone](me.id)

      def maybeMoveTowardsBoss = maybeBoss.flatMap { boss =>
        val bossPos = boss.currentPosition(startTime)

        Option.unless(
          (bossPos - currentPosition).modulus <= WithTargetAbility.meleeRange || iHaveLinvingZoneDebuff
        ) {
          val targetPos =
            bossPos + (currentPosition - bossPos).safeNormalized * WithTargetAbility.meleeRange * 0.95
          val (previousPosition, _, travelledDistance) = someDistanceInfo(startTime, me)
          preGameMovement(startTime, me, currentPosition, targetPos, travelledDistance).headOption
        }
      }.flatten

      def maybeAttackBoss = maybeBoss.flatMap { boss =>
        val maybeAggressiveAttack = defaultAggressiveAbility(currentGameState, me, startTime, boss)

        val maybeFiller = maybeEnergyKickUsage(currentGameState, startTime, me, boss)

        maybeAggressiveAttack.orElse(maybeFiller)
      }

      def maybeStopMoving = stopMoving(startTime, me, currentPosition, me.rotation)

      Vector(
        maybeMoveTowardsBoss.orElse(maybeStopMoving).orElse(maybeAttackBoss)
      ).flatten
    case Some(pos) =>
      val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)
      preGameMovement(startTime, me, currentPosition, pos, travelledDistance)
  }

}
