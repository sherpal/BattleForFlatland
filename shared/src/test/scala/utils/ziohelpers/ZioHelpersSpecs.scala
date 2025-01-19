package utils.ziohelpers

import zio.test.ZIOSpecDefault
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ZioHelpersSpecs extends ZIOSpecDefault {
  def spec = suite("failIfWith")(
    test("failIfWith should fail if condition is true") {
      for {
        condition <- ZIO.succeed(true)
        output    <- failIfWith(condition, 0).either
      } yield assertTrue(output == Left(0))
    }
  )

}
