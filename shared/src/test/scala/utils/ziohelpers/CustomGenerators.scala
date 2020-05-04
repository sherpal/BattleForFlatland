package utils.ziohelpers

import gamelogic.physics.Complex
import zio.random.Random
import zio.test.Gen

object CustomGenerators {
  val complexGen: Gen[Random, Complex] = Gen.anyDouble.zip(Gen.anyDouble).map(Complex.fromTuple)
}
