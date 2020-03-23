package services.logging

import zio.Task

object Logging {

  trait Service {

    def info(line: => String): Task[Unit]

    def debug(line: => String): Task[Unit]

    def warn(line: => String): Task[Unit]

    def error(line: => String): Task[Unit]

  }
}
