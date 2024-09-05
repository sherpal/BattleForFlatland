package application

class Logger {
  def info(m: => String): Unit = println(m)

  def warn(m: => String): Unit = println(s"[warn] $m")

  def error(m: => String): Unit = System.err.println(m)

  def error(m: => String, e: Throwable): Unit = {
    System.err.println(m)
    e.printStackTrace()
  }
}
