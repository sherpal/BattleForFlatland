package game.ai

import gamelogic.entities.{Entity, LivingEntity, MovingBody}
import gamelogic.gamestate.gameactions.MovingBodyMoves
import gamelogic.physics.Complex

package object utils {

  /**
    * Generates the [[gamelogic.gamestate.GameAction]] that the given AI must use in order to, either move towards
    * the target position, or move a little bit further from the target position.
    *
    * Basically an AI wants to be in the annulus centered at the targetPosition, with small radius the radius of its
    * shape and big radius the maxDistance.
    */
  def aiMovementToTarget(
      entityId: Entity.Id,
      time: Long,
      currentPosition: Complex,
      radius: Double,
      targetPosition: Complex,
      maxDistance: Double,
      fullSpeed: Double,
      slowSpeed: Double,
      currentlyMoving: Boolean
  ): Option[MovingBodyMoves] = {
    val toTarget         = targetPosition - currentPosition
    val distanceToTarget = toTarget.modulus

    if (distanceToTarget > maxDistance)
      Some(MovingBodyMoves(0L, time, entityId, currentPosition, toTarget.arg, toTarget.arg, fullSpeed, moving = true))
    else if (distanceToTarget < radius)
      Some(
        MovingBodyMoves(0L, time, entityId, currentPosition, (-toTarget).arg, toTarget.arg, slowSpeed, moving = true)
      )
    else if (currentlyMoving)
      Some(MovingBodyMoves(0L, time, entityId, currentPosition, toTarget.arg, toTarget.arg, fullSpeed, moving = false))
    else None

  }

}
