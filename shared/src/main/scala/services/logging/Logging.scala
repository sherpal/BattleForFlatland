package services.logging

import zio.{Task, UIO}

object Logging {

  trait Service {

    def info(line: => String): UIO[Unit]

    def debug(line: => String): UIO[Unit]

    def warn(line: => String): UIO[Unit]

    def error(line: => String): UIO[Unit]

  }
}
