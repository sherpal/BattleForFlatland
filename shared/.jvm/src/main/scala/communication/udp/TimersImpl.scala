package communication.udp

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}
import scala.util.control.NonFatal

class TimersImpl extends Timers {

  def setInterval(time: Int)(callback: => Unit): Timers.TimerHandler = {
    val future = TimersImpl.scheduler.scheduleWithFixedDelay(
      () => TimersImpl.safely(callback),
      time.toLong,
      time.toLong,
      TimeUnit.MILLISECONDS
    )
    Timers.TimerHandler(() => future.cancel(false): Unit)
  }

  def setTimeout(time: Int)(callback: => Unit): Timers.TimerHandler = {
    val future = TimersImpl.scheduler.schedule(
      new Runnable { def run(): Unit = TimersImpl.safely(callback) },
      time.toLong,
      TimeUnit.MILLISECONDS
    )
    Timers.TimerHandler(() => future.cancel(false): Unit)
  }

}

object TimersImpl {

  // an exception would otherwise silently cancel a periodic task
  private[udp] def safely(callback: => Unit): Unit =
    try callback
    catch { case NonFatal(e) => e.printStackTrace() }

  // A single daemon thread shared by all timers: callbacks must be short, and they are never run
  // concurrently with each other. A callback throwing is reported, but must not kill later timers.
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory {
      def newThread(r: Runnable): Thread = {
        val thread = new Thread(r, "udp-timers")
        thread.setDaemon(true)
        thread
      }
    }
  )

}
