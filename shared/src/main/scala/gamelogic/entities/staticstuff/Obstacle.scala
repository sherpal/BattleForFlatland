package gamelogic.entities.staticstuff

import gamelogic.entities.Entity.TeamId
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.{Entity, PolygonBody}
import gamelogic.physics.Complex
import gamelogic.physics.shape.{ConvexPolygon, Polygon}
import utils.misc.RGBColour

/**
  * An [[Obstacle]] is a fixed entity on the ground that basically does nothing, except keeping other entities from
  * going through it.
  * Also, it has its importance for spells with a target. When there is an obstacle between two entities, one can not
  * use an ability which targets the other one.
  */
final case class Obstacle(id: Entity.Id, time: Long, pos: Complex, shape: Polygon) extends PolygonBody {
  def rotation: Angle = 0.0

  def teamId: TeamId = Entity.teams.neutralTeam

  def colour: RGBColour = RGBColour.white
}

object Obstacle {

  /**
    * Generates positively oriented vertices for a rectangle roughly equivalent to a segment from `z1` to `z2`, with
    * the given `thickness`.
    */
  def segmentObstacleVertices(z1: Complex, z2: Complex, thickness: Double): Vector[Complex] = {
    val orthogonalNorm = thickness / 2 * (z2 - z1).orthogonal.normalized

    val p1 = z1 - orthogonalNorm
    val p4 = z1 + orthogonalNorm

    val p2 = z2 - orthogonalNorm
    val p3 = z2 + orthogonalNorm

    Vector(p1, p2, p3, p4) // order and signs is important for orientation
  }

  /**
    * Creates an [[Obstacle]] which is roughly a "segment" between the points `z1` and `z2`.
    * The `thickness` parameter determines how thick the obstacle will be. Something like 5 (in current game
    * coordinate system) is good for a "thin" obstacle.
    */
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
