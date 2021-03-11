package game.ai.goodais.boss.boss110

import game.ai.goodais.classes.HexagonAIController
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.entities.classes.Hexagon
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.boss.dawnoftime.Boss110

final class HexagonForBoss110(index: Int) extends HexagonAIController {

  val name: PlayerName.AIPlayerName = PlayerName.AIPlayerName(PlayerClasses.Hexagon, index)

  def loop(startingTime: Long, gameState: GameState, me: Hexagon, sendActions: List[GameAction] => Unit): Unit = {
    import gamelogic.physics.Complex.DoubleWithI

    val actions: List[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val maybeTank      = maybeTankWithNotEnoughHot(gameState, me)
        val putHot         = putHotOnFirstDefined(gameState, me, startingTime, List(maybeTank))
        val maybeFlashHeal = maybeFlashHealWithThreshold(gameState, startingTime, me, 0.5)

        putHot.orElse(maybeFlashHeal).toList
      case None =>
        val (previousPosition, currentPosition) = someDistanceInfo(startingTime, me)
        val travelledDistance                   = previousPosition distanceTo currentPosition
        val targetPosition                      = Boss110.halfHeight.i * (index - 0.5)
        preGameMovement(startingTime, me, currentPosition, targetPosition, travelledDistance)
    }

    sendActions(actions)
  }

}
