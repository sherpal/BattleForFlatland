package services.logging

import zio.UIO
import org.scalajs.dom
import zio.ZIO
import zio.ZLayer

class FLogging(console: dom.Console) extends Logging {

  override def info(line: => String): UIO[Unit] = ZIO.succeed(console.log(line))

  override def debug(line: => String): UIO[Unit] = ZIO.succeed(console.debug(line))

  override def warn(line: => String): UIO[Unit] = ZIO.succeed(console.warn(line))

  override def error(line: => String): UIO[Unit] = ZIO.succeed(console.error(line))

}

object FLogging {
  def live = ZLayer.fromZIO(for {
    console  <- ZIO.succeed(dom.console)
    flogging <- ZIO.succeed(FLogging(console))
  } yield (flogging: Logging))
}
