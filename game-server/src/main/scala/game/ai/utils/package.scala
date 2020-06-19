package game.ai

import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, LivingEntity, MovingBody, WithPosition, WithTarget, WithThreat}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.gamestate.gameactions.{ChangeTarget, MovingBodyMoves}
import gamelogic.physics.Complex
import scala.Ordering.Double.TotalOrdering

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
      currentlyMoving: Boolean,
      currentRotation: Angle
  ): Option[MovingBodyMoves] = {
    val toTarget         = targetPosition - currentPosition
    val distanceToTarget = toTarget.modulus

    if (distanceToTarget > maxDistance)
      Some(MovingBodyMoves(0L, time, entityId, currentPosition, toTarget.arg, toTarget.arg, fullSpeed, moving = true))
    else if (distanceToTarget < radius)
      Some(
        MovingBodyMoves(0L, time, entityId, currentPosition, (-toTarget).arg, toTarget.arg, slowSpeed, moving = true)
      )
    else if (currentlyMoving || currentRotation != toTarget.arg)
      Some(MovingBodyMoves(0L, time, entityId, currentPosition, toTarget.arg, toTarget.arg, fullSpeed, moving = false))
    else None

  }

  def findTarget(me: WithThreat with WithPosition, currentGameState: GameState): Option[PlayerClass] =
    me.damageThreats
      .maxByOption(_._2)
      .map(_._1)
      .flatMap(
        currentGameState.players.get // this could change in the future
      )
      .fold(currentGameState.players.values.minByOption(player => (player.pos - me.pos).modulus))(Some(_))

  def changeTarget(me: WithTarget, targetId: Entity.Id, time: Long): Option[GameAction] =
    Option.unless(targetId == me.targetId)(ChangeTarget(0L, time, me.id, targetId))

}
