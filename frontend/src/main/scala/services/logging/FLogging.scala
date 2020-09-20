package services.logging

import org.scalajs.dom
import zio.{Task, UIO, ZLayer}

object FLogging {

  val serviceLive: Logging.Service = new Logging.Service {
    def info(line: => String): Task[Unit] = Task.effect(dom.console.log(line))

    def debug(line: => String): Task[Unit] =
      if (scala.scalajs.LinkingInfo.developmentMode)
        Task.effect(dom.console.log(line))
      else UIO(())

    def warn(line: => String): Task[Unit] = Task.effect(dom.console.warn(line))

    def error(line: => String): Task[Unit] = Task.effect(dom.console.error(line))
  }

  val live: ZLayer[Any, Nothing, Logging] = ZLayer.succeed(serviceLive)

}
