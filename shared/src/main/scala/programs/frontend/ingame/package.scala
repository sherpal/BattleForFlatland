package programs.frontend

import java.util.concurrent.TimeUnit

import models.bff.ingame.InGameWSProtocol.{Ping, Pong}
import zio.clock.Clock
import zio.{Task, UIO, ZIO}

package object ingame {

  /**
    * Computes the the delta difference between this system time and the server system time.
    */
  def synchronizeClock(pingPong: Ping => UIO[Pong], tries: Int = 10): ZIO[Clock, Nothing, Double] = {
    def accumulator(remaining: Int, deltas: List[Long]): ZIO[zio.clock.Clock, Nothing, Double] =
      for {
        now <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
        ping <- UIO(Ping(now))
        fiber <- pingPong(ping).fork
        pong <- fiber.join
        nowAgain <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
        latency <- UIO((nowAgain - now) / 2)
        linkTime <- UIO(latency + pong.midwayDistantTime)
        newDeltas <- UIO((linkTime - nowAgain) +: deltas)
        delta <- if (remaining == 0) UIO(newDeltas.sum.toDouble / newDeltas.length)
        else accumulator(remaining - 1, newDeltas)
      } yield delta

    accumulator(tries, Nil)
  }

}
