package server

import java.util.concurrent.{Executors, TimeUnit}

/** Stops the game server when nobody plays anymore: it runs on the computer of a player, and nobody
  * else can stop it.
  *
  * That is when no player connected within [[NobodyCameDelay]] of the start, or when every player
  * left (the end of the game, or they lost the connection) for [[EveryoneLeftDelay]] (enough to
  * come back after a connection problem).
  */
object Watchdog {
  val NobodyCameDelay: Long   = 3 * 60 * 1000
  val EveryoneLeftDelay: Long = 60 * 1000

  def start(someoneIsConnected: () => Boolean): Unit = {
    val startedAt       = System.currentTimeMillis()
    var someoneCame     = false
    var lastTimeSomeone = startedAt
    val executor = Executors.newSingleThreadScheduledExecutor { runnable =>
      val thread = Thread(runnable, "watchdog")
      thread.setDaemon(true)
      thread
    }
    executor.scheduleAtFixedRate(
      () => {
        val now = System.currentTimeMillis()
        if someoneIsConnected() then {
          someoneCame = true
          lastTimeSomeone = now
        } else if someoneCame && now - lastTimeSomeone > EveryoneLeftDelay then {
          println("Everyone left, stopping.")
          sys.exit(0)
        } else if !someoneCame && now - startedAt > NobodyCameDelay then {
          println("Nobody came, stopping.")
          sys.exit(0)
        }
      },
      5,
      5,
      TimeUnit.SECONDS
    )
  }
}
