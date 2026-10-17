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

/** This is the same as `aiMovementToTarget`, but uses the underlying
  * [[gamelogic.physics.pathfinding.Graph]] to walk around obstacles.
  *
  * The logic for the algorithm is as follows:
  *   - If the target is close (< maxDistance) and nothing stands in between, then we do as in
  *     `aiMovementToTarget`
  *   - Otherwise, we ask the graph for the shortest path towards the target (or towards the closest
  *     legal position if the target position is not legal for us), and we head towards the first
  *     waypoint of that path.
  *
  * The path is recomputed at each call. This is fine because computing it is cheap, and because it
  * is the actual shortest path, so it does not change abruptly from one frame to the next.
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
    graph: Graph
): Option[MovingBodyMoves] = {

  val distanceToTarget = currentPosition.distanceTo(targetPosition)

  if distanceToTarget <= maxDistance && graph.isSegmentClear(currentPosition, targetPosition, 0)
  then
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
    // waypoints closer than that are considered reached, so that we don't turn around them
    val reachedDistance = 1.0
    graph
      .findPath(currentPosition, targetPosition)
      .flatMap(_.find(_.distanceTo(currentPosition) > reachedDistance)) match {
      case Some(nextWaypoint) =>
        val toWaypoint = nextWaypoint - currentPosition
        Some(
          MovingBodyMoves(
            GameAction.Id.zero,
            time,
            entityId,
            currentPosition,
            toWaypoint.arg,
            toWaypoint.arg,
            fullSpeed,
            moving = true
          )
        )
      case None =>
        // either there is no path, or we are already as close as we can be: we simply idle
        stopMoving(time, entityId, currentlyMoving, currentPosition, fullSpeed, currentRotation)
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
        GameAction.Id.zero,
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
        GameAction.Id.zero,
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
        GameAction.Id.zero,
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

def stopMoving(
    time: Long,
    entityId: Entity.Id,
    currentlyMoving: Boolean,
    currentPosition: Complex,
    speed: Double,
    rotation: Angle
): Option[MovingBodyMoves] =
  Option.when(currentlyMoving)(
    MovingBodyMoves(
      GameAction.Id.dummy,
      time,
      entityId,
      currentPosition,
      rotation,
      rotation,
      speed,
      moving = false
    )
  )

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
  Option.unless(targetId == me.targetId)(ChangeTarget(GameAction.Id.zero, time, me.id, targetId))

def maybeAbilityUsage[T <: Ability](
    me: WithAbilities,
    ability: T,
    gameState: GameState
): Option[T] =
  Option
    .when(
      me.canUseAbilityBoolean(ability, ability.time, gameState) && ability.canBeCastBoolean(
        gameState,
        ability.time
      )
    )(
      ability
    )
