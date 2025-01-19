package application.ai.goodais.bosses.boss101

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Pentagon
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses

import application.ai.utils.maybeAbilityUsage
import gamelogic.entities.boss.Boss101
import gamelogic.entities.movingstuff.PentagonBullet
import gamelogic.abilities.pentagon.CreatePentagonBullet
import application.ai.goodais.classes.PentagonAIController
import gamelogic.physics.Complex
import gamelogic.entities.Entity
import gamelogic.physics.pathfinding.Graph

final case class PentagonForBoss101(index: Int, entityId: Entity.Id)
    extends PentagonAIController(index) {

  protected def takeActions(
      gameState: GameState,
      me: Pentagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)

    val actions: Vector[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val rotationTowardsBoss = (theBoss.pos - me.pos).arg
        val maybePentagonBullet =
          maybePentagonBulletUsage(gameState, startTime, me, rotationTowardsBoss)
        maybePentagonBullet.toVector
      case None =>
        val targetPosition = Boss101.bossStartingPosition + (index - 0.5) * 300
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }

}
