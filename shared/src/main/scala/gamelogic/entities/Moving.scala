package gamelogic.entities

import gamelogic.physics.Complex

/**
  * A Moving Entity has a speed (which is its norm) and a direction towards which it moves.
  */
trait Moving extends WithPosition {
  def speed: Double
  def direction: Double
  def moving: Boolean

  override def currentPosition(currentTime: Long): Complex =
    if (moving)
      pos + (currentTime - time) * speed * Complex.rotation(direction) / 1000 // speed is per second, time is in millis
    else
      pos
}
