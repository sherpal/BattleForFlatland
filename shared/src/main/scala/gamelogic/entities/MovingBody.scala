package gamelogic.entities

import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex

trait MovingBody extends Moving with Body {

  def currentPosition(time: Long, bodies: Iterable[Body], precision: Int = 5): Complex =
    if (moving)
      lastValidPosition(super.currentPosition(time), bodies, precision)
    else
      pos

  /**
    * Moves this MovingBody at the current time, with the all moving body related attributes.
    */
  def move(time: Long, position: Complex, direction: Angle, rotation: Angle, speed: Double, moving: Boolean): MovingBody

}
