package gamelogic.entities

import gamelogic.physics.Complex

/**
  * A WithPosition Entity has a `pos` value that determines their place in the world.
  */
trait WithPosition extends Entity {
  val pos: Complex
}
