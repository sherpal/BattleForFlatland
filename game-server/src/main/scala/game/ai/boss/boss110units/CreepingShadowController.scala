package game.ai.boss.boss110units

import game.ai.boss.AIController
import gamelogic.entities.boss.boss110.CreepingShadow
import gamelogic.gamestate.gameactions.boss110.AddCreepingShadow
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import gamelogic.entities.Entity
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.EntityRadiusChange
import gamelogic.gamestate.gameactions.MovingBodyMoves

object CreepingShadowController extends AIController[CreepingShadow, AddCreepingShadow] {

  override protected def takeActions(
      currentGameState: GameState,
      me: CreepingShadow,
      currentPosition: Complex,
      startTime: Long,
      lastTimeStamp: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): List[GameAction] = {

    val (position, radius) = CreepingShadow.computePositionAndRadius(currentGameState, startTime)

    val maybeChangeRadius = Option.when(me.radius != radius)(EntityRadiusChange(0L, startTime, me.id, radius))

    val elapsed             = startTime - lastTimeStamp
    val theoreticalDistance = me.speed * elapsed / 1000
    val distanceToTarget    = currentPosition.distanceTo(position)

    val maybeMoving =
      if (me.moving && distanceToTarget < theoreticalDistance) {
        Some(MovingBodyMoves(0L, startTime, me.id, position, 0, 0, me.speed, moving = false))
      } else if (distanceToTarget > theoreticalDistance) {
        val action = MovingBodyMoves(
          0L,
          startTime,
          me.id,
          currentPosition,
          (position - currentPosition).arg,
          0,
          me.speed,
          moving = true
        )
        Some(action)
      } else None

    List(
      maybeMoving,
      maybeChangeRadius
    ).flatten

  }

  override protected def getMe(gameState: GameState, entityId: Entity.Id): Option[CreepingShadow] =
    gameState.entities.get(entityId).collect { case cs: CreepingShadow => cs }

}
