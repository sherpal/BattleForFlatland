package utils.ziohelpers

import zio.test.{DefaultRunnableSpec, ZSpec}
import zio._
import zio.console._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._

object ZioHelpersSpecs extends DefaultRunnableSpec {
  def spec = suite("failIfWith")(
    testM("failIfWith should fail if condition is true") {
      for {
        condition <- UIO.succeed(true)
        output <- failIfWith(condition, 0).either
      } yield assert(output)(equalTo(Left(0)))
    }
  )

}
