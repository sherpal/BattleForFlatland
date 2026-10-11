package gamelogic.entities

import gamelogic.physics.Complex
import gamelogic.utils.OpaqueDoubleCompanion

/** A WithPosition Entity has a `pos` value that determines their place in the world.
  */
trait WithPosition extends Entity {
  val pos: Complex

  def currentPosition(time: Long): Complex = pos
}

object WithPosition {

  opaque type Angle = Double
  object Angle extends OpaqueDoubleCompanion[Angle] {
    extension (x: Angle) {
      inline def +(y: Angle): Angle = x + y
      inline def -(y: Angle): Angle = x - y

      inline def *(y: Angle): Angle = x * y

      inline def /(cut: Int): Angle = x / cut

      inline def unary_- : Angle = -x
    }

    inline def Pi: Angle = math.Pi

    val Tau: Angle = 2 * Pi
  }

}
