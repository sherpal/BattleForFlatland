package application.ai.goodais.bosses.boss102

import application.ai.goodais.classes.HexagonAIController
import gamelogic.entities.classes.Hexagon
import gamelogic.physics.Complex
import gamelogic.entities.Entity.Id
import gamelogic.physics.pathfinding.Graph
import gamelogic.gamestate.GameAction
import gamelogic.gamestate.GameState
import gamelogic.entities.Entity
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.buffs.Buff
import gamelogic.abilities.hexagon.HexagonHot
import gamelogic.buffs.HoT
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.classes.Square

final case class HexagonForBoss102(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with Boss102GoodAIController[Hexagon] {

  override protected def takeActions(
      currentGameState: GameState,
      me: Hexagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = findSafeSpot(currentGameState, startTime) match {
    case None =>
      val maybeStopMoving = stopMoving(startTime, me, currentPosition, me.rotation)

      def maybePutHot = putHotOnFirstDefined(
        currentGameState,
        me,
        startTime,
        currentGameState.players.values.toList
          .filter(_.cls == PlayerClasses.Square)
          .map(square =>
            Option.when(countOfMyHotOnEntity(currentGameState, square.id, me) < 2)(square)
          ) ++
          currentGameState.players.values.toList.map(player =>
            Option.when(
              countOfMyHotOnEntity(
                currentGameState,
                player.id,
                me
              ) == 0 && player.life / player.maxLife < 0.5
            )(player)
          )
      )

      def maybeFlashHeal = currentGameState.players.values
        .collect {
          case square: Square if square.life / square.maxLife < 0.5 => square
        }
        .flatMap(square => maybeFlashHealUsage(currentGameState, startTime, square, me))
        .headOption

      Vector(
        maybeStopMoving,
        maybeFlashHeal.orElse(maybePutHot)
      ).flatten
    case Some(pos) =>
      val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)
      preGameMovement(startTime, me, currentPosition, pos, travelledDistance)
  }

}
