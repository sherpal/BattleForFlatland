package services.logging

import zio.ZIO

final class Log private[logging] () {
  def debug(line: => String): ZIO[Logging, Throwable, Unit] = ZIO.accessM(_.get.debug(line))
  def info(line: => String): ZIO[Logging, Throwable, Unit]  = ZIO.accessM(_.get.info(line))
  def warn(line: => String): ZIO[Logging, Throwable, Unit]  = ZIO.accessM(_.get.warn(line))
  def error(line: => String): ZIO[Logging, Throwable, Unit] = ZIO.accessM(_.get.error(line))

}
