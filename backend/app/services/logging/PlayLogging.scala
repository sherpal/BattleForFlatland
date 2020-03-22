package services.logging

import play.api.Logger
import zio.{Has, Layer, Task, ZLayer}

object PlayLogging {

  def live(logger: Logger): Layer[Nothing, Has[Logging.Service]] =
    ZLayer.succeed(new Logging.Service {
      def debug(line: => String): Task[Unit] = Task.effect(logger.debug(line))
      def info(line: => String): Task[Unit]  = Task.effect(logger.info(line))
      def warn(line: => String): Task[Unit]  = Task.effect(logger.warn(line))
      def error(line: => String): Task[Unit] = Task.effect(logger.error(line))
    })

}
