package communication.udp

import scala.scalajs.js.timers

class TimersImpl extends Timers {

  override def setInterval(time: Int)(callback: => Unit): Timers.TimerHandler = {
    val handle = timers.setInterval(time.toDouble)(callback)
    Timers.TimerHandler { () =>
      timers.clearInterval(handle)
    }
  }

  override def setTimeout(time: Int)(callback: => Unit): Timers.TimerHandler = {
    val handle = timers.setTimeout(time.toDouble)(callback)
    Timers.TimerHandler { () =>
      timers.clearTimeout(handle)
    }
  }

}
