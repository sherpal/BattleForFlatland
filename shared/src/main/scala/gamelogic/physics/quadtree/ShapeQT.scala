package gamelogic.physics.quadtree

import gamelogic.entities.PolygonBody
import gamelogic.physics.Complex
import gamelogic.physics.shape.{Polygon, Shape}

/**
  * A [[ShapeQT]] is supposed to be an abstraction on top of a list of [[gamelogic.entities.PolygonBody]].
  * It is used for path finding algorithm by the AIs. Obstacles in the game will be polygons by design, because
  * it is much easier to work with.
  *
  * In the future, this class my be a tiny bit more optimize than a simple wrapper around a list of shapes.
  */
final class ShapeQT(val shapes: List[PolygonBody]) {

  /**
    * Adds a new shape.
    */
  def :+(shape: PolygonBody): ShapeQT = new ShapeQT(shape +: shapes)

  /** Checks whether there is a shape among these colliding the other given shape. */
  def collides(shape: Shape, translation: Complex, rotation: Double): Boolean =
    shapes.exists(body => body.collidesShape(shape, translation, rotation, 0))

  /** Checks whether the given shape is in here. */
  def contains(shape: Shape): Boolean = shapes.contains(shape)

  /** Checks whether there exists a shape here containing the given point. */
  def contains(point: Complex): Boolean = shapes.exists(_.shape.contains(point))

}
