package application.ai.goodais.bosses.boss104

import application.ai.goodais.GoodAIController
import gamelogic.entities.boss.boss104.DeathMarkTriangle
import gamelogic.entities.classes.Constants
import gamelogic.entities.{MovingBody, WithPosition}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape

/** Shared behaviour of every Boss104 bot during the two stages of the [[DeathMarkTriangle]] attack.
  * Surviving it is the only thing that matters while it is up, so all four classes check this first
  * and forget about their usual job.
  *
  *   - first stage (inside is deadly): go and stand just outside the closest edge. That is both
  *     safe and the best possible spot for what comes next.
  *   - second stage (outside is deadly): run to the centre of the triangle.
  *
  * There is nothing to do once the triangle is gone: `maybeHandleDeathMarkTriangle` returns
  * [[scala.None]] and each bot naturally falls back to its normal behaviour, which for the
  * Pentagons means walking back to their base position.
  */
trait DeathMarkTriangleAware[EntityType <: MovingBody & WithPosition] {
  self: GoodAIController[EntityType] =>

  /** How far outside the triangle a bot stands during the first stage. It is the centre of a player
    * that decides whether they are hit, so one radius would already do; we take a bit more to
    * absorb the fact that they are still moving when the attack resolves.
    */
  private inline def safetyMargin: Double = Constants.playerRadius * 3

  /** Below that distance to the spot we aim for, we consider ourselves arrived and stop moving
    * (rather than emitting a movement action on every single loop).
    */
  private inline def closeEnough: Double = 5.0

  def maybeHandleDeathMarkTriangle(
      gameState: GameState,
      me: EntityType,
      time: Long
  ): Option[Vector[GameAction]] =
    gameState.allTEntities[DeathMarkTriangle].values.headOption.map { triangle =>
      val (_, currentPosition, travelledDistance) = someDistanceInfo(time, me)

      val whereIWantToBe =
        if triangle.insideIsDeadly then justOutsideClosestEdge(triangle, currentPosition)
        else triangle.pos

      if currentPosition.distanceTo(whereIWantToBe) < closeEnough then
        stopMoving(time, me, currentPosition, me.rotation).toVector
      else preGameMovement(time, me, currentPosition, whereIWantToBe, travelledDistance)
    }

  /** The point [[safetyMargin]] outside of the edge of the triangle that is the closest to `from`.
    *
    * We take the closest point of each of the three edges, keep the nearest one, and push it away
    * from the centre of the triangle.
    */
  private def justOutsideClosestEdge(triangle: DeathMarkTriangle, from: Complex): Complex = {
    val vertices = triangle.vertices
    val closestOnBoundary = vertices
      .zip(vertices.tail :+ vertices.head)
      .map(Shape.closestToSegment(_, from))
      .minBy(_.distanceTo(from))

    closestOnBoundary + safetyMargin * (closestOnBoundary - triangle.pos).normalized
  }

}
