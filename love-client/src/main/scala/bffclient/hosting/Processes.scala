package bffclient.hosting

import scala.concurrent.{ExecutionContext, Future}

import bffclient.lua.ProcessModule

/** Commands, run on a LÖVE thread (`process.lua`): `run(command)` gives their output (stdout and
  * stderr, together). They are commands of the shell on Linux and macOS, and command lines run
  * without a shell on Windows (whose console window would show up over the game).
  */
object Processes extends WorkerResults(ProcessModule) {

  /** Starts the command in the background (it outlives us), its output going to the file `log`.
    * Completes once it started.
    */
  def spawn(command: String, log: String)(using ExecutionContext): Future[Unit] =
    result(ProcessModule.spawn(command, log)).map(_ => ())
}
