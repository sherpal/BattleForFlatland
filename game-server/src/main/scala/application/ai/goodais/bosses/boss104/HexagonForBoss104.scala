package application.ai.goodais.bosses.boss104

import application.ai.goodais.classes.HexagonAIController
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.boss.dawnoftime.Boss104
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

final case class HexagonForBoss104(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index) {

  protected def takeActions(
      gameState: GameState,
      me: Hexagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)

    val actions: Vector[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val maybeTank = maybeTankWithNotEnoughHot(gameState, me)

        val entityWithBossDebuffAndNoHotFromMe = gameState.players.values.find { player =>
          val buffs = gameState.allBuffsOfEntity(player.id)
          buffs.count(_.resourceIdentifier == Buff.boss101BigDotIdentifier) > countOfMyHotOnEntity(
            gameState,
            player.id,
            me
          )
        }

        val putHot =
          putHotOnFirstWithThreshold(
            gameState,
            me,
            startTime,
            0.5
          )

        val maybeFlashHeal = maybeFlashHealWithThreshold(gameState, startTime, me, 0.3)

        putHot.orElse(maybeFlashHeal).toVector

      case None =>
        val targetPosition = Boss104.bossStartingPosition - 200.i + (index - 0.5) * 60
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }

}
