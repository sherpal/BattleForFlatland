package gamelogic.entities

import gamelogic.physics.Complex

trait MovingBody extends Moving with Body {

  def currentPosition(time: Long, bodies: Iterable[Body], precision: Int = 5): Complex =
    if (moving)
      lastValidPosition(super.currentPosition(time), bodies, precision)
    else
      pos

  /**
    * Checks if this MovingBody collides with that Moving body.
    * @param that     other MovingBody
    * @param thisTime the time elapsed since this object position
    * @param thatTime the time elapsed since that object position
    * @return
    */
  def collides(that: MovingBody, thisTime: Long, thatTime: Long): Boolean =
    shape.collides(currentPosition(thisTime), rotation, that.shape, that.currentPosition(thatTime), that.rotation)

  def collides(that: Body, thisTime: Long): Boolean =
    shape.collides(currentPosition(thisTime), rotation, that.shape, that.pos, that.rotation)

}
