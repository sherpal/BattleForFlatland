package gamelogic.physics.quadtree

import gamelogic.entities.{Entity, PolygonBody}
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

  /** Adds all the new shapes. We prepend to have a O(length(newShapes)). Most of the time they are empty. */
  def ++(newShapes: List[PolygonBody]): ShapeQT = new ShapeQT(newShapes ++ shapes)

  /**
    * Removes the shapes with given ids. We check whether the ids that we want to remove are empty to increase
    * performance. That way, removing the empty list is O(1) (which happens all the time).
    */
  def --(ids: List[Entity.Id]): ShapeQT = if (ids.isEmpty) this else new ShapeQT(shapes.filterNot(ids contains _.id))

  /** Checks whether there is a shape among these colliding the other given shape. */
  def collides(shape: Shape, translation: Complex, rotation: Double): Boolean =
    shapes.exists(body => body.collidesShape(shape, translation, rotation, 0))

  /** Checks whether the given shape is in here. */
  def contains(shape: Shape): Boolean = shapes.contains(shape)

  /** Checks whether there exists a shape here containing the given point. */
  def contains(point: Complex): Boolean = shapes.exists(_.containsPoint(point, 0))

}

object ShapeQT {
  def empty: ShapeQT = new ShapeQT(Nil)
}
