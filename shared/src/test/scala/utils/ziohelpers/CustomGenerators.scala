package utils.ziohelpers

import gamelogic.physics.Complex
import zio.random.Random
import zio.test.Gen

object CustomGenerators {
  val complexGen: Gen[Random, Complex] =
    for {
      real <- Gen.anyDouble
      imag <- Gen.anyDouble
    } yield Complex(real, imag)

}
