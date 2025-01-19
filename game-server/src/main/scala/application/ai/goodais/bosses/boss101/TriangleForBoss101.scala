package application.ai.goodais.bosses.boss101

import application.ai.goodais.GoodAIController
import gamelogic.entities.classes.Triangle
import scala.reflect.ClassTag
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.outofgame.PlayerClasses
import gamelogic.entities.classes.Constants
import gamelogic.entities.boss.Boss101
import gamelogic.buffs.Buff
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.WithTargetAbility
import gamelogic.abilities.triangle.UpgradeDirectHit
import gamelogic.abilities.triangle.DirectHit
import gamelogic.abilities.triangle.EnergyKick
import gamelogic.gamestate.gameactions.MovingBodyMoves
import application.ai.goodais.classes.TriangleAIController
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import gamelogic.entities.Entity

final case class TriangleForBoss101(index: Int, entityId: Entity.Id)
    extends TriangleAIController(index) {

  protected def takeActions(
      gameState: GameState,
      me: Triangle,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)

    val actions: Vector[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss)
          if theBoss.pos.distanceTo(currentPosition) < WithTargetAbility.meleeRange =>
        val maybeAggressiveAttack = defaultAggressiveAbility(gameState, me, startTime, theBoss)

        val maybeFiller = maybeEnergyKickUsage(gameState, startTime, me, theBoss)

        Vector(
          Option.when(me.moving)(
            MovingBodyMoves(
              GameAction.Id.dummy,
              startTime,
              me.id,
              currentPosition,
              (theBoss.pos - me.pos).arg,
              (theBoss.pos - me.pos).arg,
              me.speed,
              moving = false
            )
          ),
          maybeFiller,
          maybeAggressiveAttack
        ).flatten

      case Some(theBoss) =>
        val targetPosition = theBoss.pos + theBoss.shape.radius + me.shape.radius
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
      case None =>
        val targetPosition = Boss101.bossStartingPosition + 60
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }

}
