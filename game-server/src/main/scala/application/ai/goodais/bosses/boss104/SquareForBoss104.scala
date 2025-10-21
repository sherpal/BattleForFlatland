package application.ai.goodais.bosses.boss104

import gamelogic.entities.Entity
import application.ai.goodais.classes.SquareAIController
import gamelogic.physics.Complex
import gamelogic.entities.classes.Square
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.entities.boss.dawnoftime.Boss104

final case class SquareForBoss104(index: Int, entityId: Entity.Id)
    extends SquareAIController(index) {

  override protected def takeActions(
      gameState: GameState,
      me: Square,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)

    val actions: Vector[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val shouldIHammer = isMyThreatTowardsTargetEnough(theBoss, me)

        val maybeTaunt = Option
          .unless(shouldIHammer)(maybeTauntUsage(gameState, startTime, me, theBoss))
          .flatten
        val maybeEnrage = maybeEnrageUsage(
          gameState,
          startTime,
          me,
          me.resourceAmount.amount < 5 && me.life > 180 && !alreadyEnraged(gameState, me)
        )

        val maybeHammerHit = Option
          .when(shouldIHammer)(maybeHammerHitUsage(gameState, startTime, me, theBoss))
          .flatten
        maybeHammerHit.orElse(maybeTaunt).orElse(maybeEnrage).toVector
      case None =>
        val targetPosition = Boss104.bossStartingPosition - 50.i
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }
}
