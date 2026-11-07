package application

trait TimeManager {

  /** Waits for `millis` milliseconds (not at all if it is not positive).
    *
    * This used to spin (yielding) until the time was up: each loop using it (the game master's and
    * the AI managers') then kept a core busy for the whole game, and the game server runs on the
    * computer of one of the players, next to their client.
    */
  protected def sleep(millis: Long): Unit =
    if millis > 0 then Thread.sleep(millis)

}
