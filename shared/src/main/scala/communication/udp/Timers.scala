package communication.udp

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future, Promise}

trait Timers {

  def setInterval(time: Int)(callback: => Unit): Timers.TimerHandler

  def setTimeout(time: Int)(callback: => Unit): Timers.TimerHandler

  def now(): Long = System.currentTimeMillis()

  def within[A](error: String, ms: Int)(future: Future[A])(using
      ExecutionContext
  ): Future[A] = {
    val promise = Promise[A]()
    val handle  = setTimeout(ms)(promise.tryFailure(IOException(error)))
    future.onComplete { result =>
      handle.cancel()
      promise.tryComplete(result)
    }
    promise.future
  }

}

object Timers {
  class TimerHandler(val cancel: () => Unit)
}
