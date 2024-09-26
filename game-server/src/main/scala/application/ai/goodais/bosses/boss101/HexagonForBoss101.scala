package application.ai.goodais.bosses.boss101

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Hexagon
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.Boss101
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.hexagon.HexagonHot
import gamelogic.entities.classes.Square
import gamelogic.abilities.boss.boss101.BigDot
import gamelogic.buffs.Buff
import gamelogic.buffs.DoT
import gamelogic.entities.Entity
import gamelogic.buffs.HoT
import gamelogic.abilities.hexagon.FlashHeal
import application.ai.goodais.classes.HexagonAIController
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

final case class HexagonForBoss101(index: Int, entityId: Entity.Id)
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
          putHotOnFirstDefined(
            gameState,
            me,
            startTime,
            List(maybeTank, entityWithBossDebuffAndNoHotFromMe)
          )

        val maybeFlashHeal = maybeFlashHealWithThreshold(gameState, startTime, me, 0.5)

        putHot.orElse(maybeFlashHeal).toVector

      case None =>
        val targetPosition = Boss101.bossStartingPosition - 200.i + (index - 0.5) * 60
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }

}
