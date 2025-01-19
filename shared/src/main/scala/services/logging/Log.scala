package services.logging

import zio.ZIO

final class Log private[logging] () {
  def debug(line: => String): ZIO[Logging, Nothing, Unit] = ZIO.serviceWithZIO[Logging](_.debug(line))
  def info(line: => String): ZIO[Logging, Nothing, Unit]  = ZIO.serviceWithZIO[Logging](_.info(line))
  def warn(line: => String): ZIO[Logging, Nothing, Unit]  = ZIO.serviceWithZIO[Logging](_.warn(line))
  def error(line: => String): ZIO[Logging, Nothing, Unit] = ZIO.serviceWithZIO[Logging](_.error(line))
}
