package services.logging

import play.api.Logger
import zio.{Has, Layer, Task, UIO, ZLayer}

object PlayLogging {

  def live(logger: Logger): Layer[Nothing, Has[Logging.Service]] =
    ZLayer.succeed(new Logging.Service {
      def debug(line: => String): UIO[Unit] = UIO.effectTotal(logger.debug(line))
      def info(line: => String): UIO[Unit]  = UIO.effectTotal(logger.info(line))
      def warn(line: => String): UIO[Unit]  = UIO.effectTotal(logger.warn(line))
      def error(line: => String): UIO[Unit] = UIO.effectTotal(logger.error(line))
    })

}
