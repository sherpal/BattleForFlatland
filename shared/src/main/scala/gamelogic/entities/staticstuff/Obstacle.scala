package gamelogic.entities.staticstuff

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Body, Entity}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Polygon}
import utils.misc.RGBColour

/**
  * An [[Obstacle]] is a fixed entity on the ground that basically does nothing, except keeping other entities from
  * going through it.
  * Also, it has its importance for spells with a target. When there is an obstacle between two entities, one can not
  * use an ability which targets the other one.
  */
final case class Obstacle(id: Entity.Id, time: Long, pos: Complex, shape: Polygon) extends Body {
  def rotation: Angle = 0.0

  def teamId: TeamId = Entity.teams.neutralTeam

  def colour: RGBColour = RGBColour.white

}

object Obstacle {

  def segmentObstacleVertices(z1: Complex, z2: Complex, thickness: Double): Vector[Complex] = {
    val orthogonalNorm = thickness / 2 * (z2 - z1).orthogonal.normalized

    val p1 = z1 - orthogonalNorm
    val p4 = z1 + orthogonalNorm

    val p2 = z2 - orthogonalNorm
    val p3 = z2 + orthogonalNorm

    Vector(p1, p2, p3, p4) // order and signs is important for orientation
  }

  def segmentObstacle(
      id: Entity.Id,
      time: Long,
      position: Complex,
      z1: Complex,
      z2: Complex,
      thickness: Double
  ): Obstacle =
    Obstacle(id, time, position, new ConvexPolygon(segmentObstacleVertices(z1, z2, thickness)))

}
