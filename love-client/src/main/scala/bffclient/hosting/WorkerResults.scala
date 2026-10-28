package bffclient.hosting

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.{Failure, Success}

import bffclient.lua.WorkerModule

/** Runs the inputs of a [[WorkerModule]] (e.g. `process.lua`), whose results arrive in `Future`s
  * (failed with its error, if there is one). They are polled every frame by a timer while some are
  * pending.
  */
class WorkerResults[I, R](module: WorkerModule[I, R]) {
  private val pending = mutable.Map.empty[Int, Promise[R]]
  private var poller  = Option.empty[js.timers.SetIntervalHandle]

  def run(input: I): Future[R] = {
    val promise = Promise[R]()
    pending(module.run(input)) = promise
    if poller.isEmpty then poller = Some(js.timers.setInterval(1)(poll()))
    promise.future
  }

  private def poll(): Unit = {
    @tailrec def drain(): Unit = module.poll().toOption match {
      case None => ()
      case Some(result) =>
        pending
          .remove(result.id)
          .foreach(
            _.complete(
              if result.error.isEmpty then Success(result.result)
              else Failure(Exception(result.error))
            )
          )
        drain()
    }
    drain()
    if pending.isEmpty then {
      poller.foreach(js.timers.clearInterval)
      poller = None
    }
  }
}
