package application.ai.utils

import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, WithAbilities, WithPosition, WithTarget, WithThreat}
import gamelogic.gamestate.gameactions.{ChangeTarget, MovingBodyMoves}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.Ordering.Double.TotalOrdering
import gamelogic.abilities.Ability
import gamelogic.gamestate.gameactions.EntityStartsCasting

/** This is the same as `aiMovementToTarget`, but uses the underlying
  * [[gamelogic.physics.pathfinding.Graph]] to walk in.
  *
  * The logic for the algorithm is as follows:
  *   - If the target position is close (< maxDistance), then we do as in `aiMovementToTarget`
  *   - If not, we check whether the `targetPosition` is a legal position.
  *   - If the target position is legal, we use the A* algorithm on the graph (adding to it the
  *     current and target positions
  *   - If the target position is not legal, we do the same but with the position in the graph who
  *     is the closest to the target position.
  *
  * The requirement on the given graph is that any point on the graph is a legal position (on the
  * graph means vertices *and* edges.
  *
  * @param isPositionLegal
  *   for a given position, returns whether this ai unit can be there.
  */
def aiMovementToTargetWithGraph(
    entityId: Entity.Id,
    time: Long,
    timeSinceLastFrame: Long,
    currentPosition: Complex,
    radius: Double,
    targetPosition: Complex,
    maxDistance: Double,
    fullSpeed: Double,
    slowSpeed: Double,
    currentSpeed: Double,
    currentlyMoving: Boolean,
    currentRotation: Angle,
    graph: Graph,
    isPositionLegal: Complex => Boolean
): Option[MovingBodyMoves] = {

  val distanceToTarget = currentPosition.distanceTo(targetPosition)

  if distanceToTarget <= maxDistance then
    aiMovementToTarget(
      entityId,
      time,
      timeSinceLastFrame,
      currentPosition,
      radius,
      targetPosition,
      maxDistance,
      fullSpeed,
      slowSpeed,
      currentSpeed,
      currentlyMoving,
      currentRotation
    )
  else {

    val maybeActualTargetPosition =
      if (isPositionLegal(targetPosition)) Some(targetPosition)
      else graph.closestPointTo(targetPosition)

    maybeActualTargetPosition
      .flatMap(actualTargetPosition =>
        graph
          .addVertices(currentPosition, actualTargetPosition)
          .euclideanA_*(currentPosition, actualTargetPosition)
      )
      .map {
        case start :: next :: _ =>
          val toTarget = next - start
          MovingBodyMoves(
            0L,
            time,
            entityId,
            currentPosition,
            toTarget.arg,
            toTarget.arg,
            fullSpeed,
            moving = true
          )
        case _ =>
          // didn't find a path, we simply idle
          MovingBodyMoves(
            0L,
            time,
            entityId,
            currentPosition,
            currentRotation,
            currentRotation,
            fullSpeed,
            moving = false
          )
      }
  }

}

/** Generates the [[gamelogic.gamestate.GameAction]] that the given AI must use in order to, either
  * move towards the target position, or move a little bit further from the target position.
  *
  * Basically an AI wants to be in the annulus centered at the targetPosition, with small radius the
  * radius of its shape and big radius the maxDistance.
  */
def aiMovementToTarget(
    entityId: Entity.Id,
    time: Long,
    timeSinceLastFrame: Long,
    currentPosition: Complex,
    radius: Double,
    targetPosition: Complex,
    maxDistance: Double,
    fullSpeed: Double,
    slowSpeed: Double,
    currentSpeed: Double,
    currentlyMoving: Boolean,
    currentRotation: Angle
): Option[MovingBodyMoves] = {
  val toTarget         = targetPosition - currentPosition
  val distanceToTarget = toTarget.modulus

  if (distanceToTarget > maxDistance)
    Some(
      MovingBodyMoves(
        0L,
        time,
        entityId,
        currentPosition,
        toTarget.arg,
        toTarget.arg,
        fullSpeed,
        moving = true
      )
    )
  else if (distanceToTarget < radius)
    Some(
      MovingBodyMoves(
        0L,
        time,
        entityId,
        currentPosition,
        (-toTarget).arg,
        toTarget.arg,
        slowSpeed,
        moving = true
      )
    )
  else if (currentlyMoving || currentRotation != toTarget.arg)
    Some(
      MovingBodyMoves(
        0L,
        time,
        entityId,
        currentPosition,
        toTarget.arg,
        toTarget.arg,
        fullSpeed,
        moving = false
      )
    )
  else None

}

def findTarget(me: WithThreat & WithPosition, currentGameState: GameState): Option[PlayerClass] =
  (for {
    biggestThreat <- me.damageThreats.maxByOption(_._2)
    biggestThreatId = biggestThreat._1
    biggestThreatAsPlayer <- currentGameState.players.get(
      biggestThreatId
    ) // this could change in the future
  } yield biggestThreatAsPlayer)
    .fold(currentGameState.players.values.minByOption(player => (player.pos - me.pos).modulus))(
      Some(_)
    )

def changeTarget(me: WithTarget, targetId: Entity.Id, time: Long): Option[ChangeTarget] =
  Option.unless(targetId == me.targetId)(ChangeTarget(0L, time, me.id, targetId))

def maybeAbilityUsage[T <: Ability](
    me: WithAbilities,
    ability: T,
    gameState: GameState
): Option[T] =
  Option
    .when(
      me.canUseAbilityBoolean(ability, ability.time) && ability.canBeCastBoolean(
        gameState,
        ability.time
      )
    )(
      ability
    )
