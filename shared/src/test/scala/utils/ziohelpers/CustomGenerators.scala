package utils.ziohelpers

import gamelogic.physics.Complex
import zio.test.Gen

object CustomGenerators {
  val complexGen =
    for {
      real <- Gen.double
      imag <- Gen.double
    } yield Complex(real, imag)

}
