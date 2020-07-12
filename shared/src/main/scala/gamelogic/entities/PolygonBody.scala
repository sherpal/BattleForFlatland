package gamelogic.entities

import gamelogic.physics.shape.Polygon

/**
  * This is a [[gamelogic.entities.Body]] whose `shape` instance is a [[gamelogic.physics.shape.Polygon]].
  */
trait PolygonBody extends Body {

  def shape: Polygon

}
