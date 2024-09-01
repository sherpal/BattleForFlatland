package programs.frontend.ingame

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import models.bff.Routes
import models.bff.ingame.InGameWSProtocol.{Ping, Pong}
import models.users.User
import services.http.*
import utils.ziohelpers.unsuccessfulStatusCode
import zio.{UIO, ZIO}
import models.bff.ingame.GameUserCredentials

/** Computes the the delta difference between this system time and the server system time.
  */
def synchronizeClock(pingPong: Ping => UIO[Pong], tries: Int = 10): ZIO[Any, Nothing, Double] = {
  def accumulator(remaining: Int, deltas: List[Long]): ZIO[Any, Nothing, Double] =
    for {
      now       <- zio.Clock.currentTime(TimeUnit.MILLISECONDS)
      ping      <- ZIO.succeed(Ping(now))
      fiber     <- pingPong(ping).fork
      pong      <- fiber.join
      nowAgain  <- zio.Clock.currentTime(TimeUnit.MILLISECONDS)
      latency   <- ZIO.succeed((nowAgain - pong.originalSendingTime) / 2)
      linkTime  <- ZIO.succeed(latency + pong.midwayDistantTime)
      newDeltas <- ZIO.succeed((linkTime - nowAgain) +: deltas)
      delta <-
        if (remaining == 0) ZIO.succeed(newDeltas.sum.toDouble / newDeltas.length)
        else accumulator(remaining - 1, newDeltas)
    } yield delta

  accumulator(tries, Nil)
}

def cancelGame(user: User, gameId: String, token: String): ZIO[HttpClient, ErrorADT, Unit] =
  (for {
    gameCredentials <- ZIO.succeed(GameUserCredentials(user.userId, gameId, token))
    code            <- postIgnore(Routes.inGameCancel, gameCredentials)
    _               <- unsuccessfulStatusCode(code)
  } yield ()).refineOrDie(ErrorADT.onlyErrorADT)
