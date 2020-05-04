package gamelogic.entities

import gamelogic.physics.Complex

/**
  * A Moving Entity has a speed (which is its norm) and a direction towards which it moves.
  */
trait Moving extends WithPosition {
  val speed: Double
  val direction: Double
  val moving: Boolean

  def currentPosition(currentTime: Long): Complex =
    if (moving)
      pos + (currentTime - time) * speed * Complex.rotation(direction) / 1000 // speed is per second, time is in millis
    else
      pos
}
