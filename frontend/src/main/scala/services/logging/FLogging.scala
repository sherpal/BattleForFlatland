package services.logging

import org.scalajs.dom
import zio.{Task, UIO, ZLayer}

object FLogging {

  val serviceLive: Logging.Service = new Logging.Service {
    def info(line: => String): UIO[Unit] = UIO.effectTotal(dom.console.log(line))

    def debug(line: => String): UIO[Unit] =
      if (scala.scalajs.LinkingInfo.developmentMode)
        UIO.effectTotal(dom.console.log(line))
      else UIO(())

    def warn(line: => String): UIO[Unit] = UIO.effectTotal(dom.console.warn(line))

    def error(line: => String): UIO[Unit] = UIO.effectTotal(dom.console.error(line))
  }

  val live: ZLayer[Any, Nothing, Logging] = ZLayer.succeed(serviceLive)

}
