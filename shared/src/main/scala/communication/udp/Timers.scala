package communication.udp

trait Timers {

  def setInterval(time: Int)(callback: => Unit): Timers.TimerHandler

  def setTimeout(time: Int)(callback: => Unit): Timers.TimerHandler

  def now(): Long = System.currentTimeMillis()

}

object Timers {
  class TimerHandler private[udp] (val cancel: () => Unit)
}
