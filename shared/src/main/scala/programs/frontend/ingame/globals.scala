package programs.frontend.ingame

import java.util.concurrent.TimeUnit

import errors.ErrorADT
import models.bff.Routes
import models.bff.ingame.InGameWSProtocol.{Ping, Pong}
import models.users.User
import services.http.*
import utils.ziohelpers.unsuccessfulStatusCode
import zio.*
import models.bff.ingame.GameUserCredentials
import models.bff.ingame.ClockSynchronizationReport

/** Computes the delta difference between this system time and the server system time.
  */
def synchronizeClock(
    pingPong: Ping => UIO[Pong],
    tries: Int = 100,
    postedOnProgress: Int => ZIO[Any, Nothing, Unit] = _ => ZIO.unit
): ZIO[Any, Nothing, ClockSynchronizationReport] = {

  case class PongTookTooLong(ping: Ping)
  def accumulator(
      remaining: Int,
      report: ClockSynchronizationReport
  ): ZIO[Any, Nothing, ClockSynchronizationReport] =
    (for {
      _     <- postedOnProgress(100 - remaining * 100 / tries)
      now   <- Clock.currentTime(TimeUnit.MILLISECONDS)
      ping  <- ZIO.succeed(Ping(now))
      fiber <- pingPong(ping).fork
      maybePong <- fiber.join
        .map(Right(_))
        .race(
          Clock.sleep(2.seconds) *> ZIO.succeed(println("Oupsy, lost one :(")) *> ZIO
            .succeed(Left(PongTookTooLong(ping)))
        )
      nowAgain <- Clock.currentTime(TimeUnit.MILLISECONDS)
      pong     <- ZIO.fromEither(maybePong)
      result <- ZIO.succeed(
        ClockSynchronizationReport
          .PingPongSuccess(ping.sendingTime, pong.midwayDistantTime, nowAgain)
      )
      updatedReport <- ZIO.succeed(report.add(result))
      report <-
        if remaining == 0 then ZIO.succeed(updatedReport)
        else Clock.sleep(10.millis) *> accumulator(remaining - 1, updatedReport)
    } yield report).catchAll { case PongTookTooLong(ping) =>
      val result =
        ClockSynchronizationReport.PingPongFailure(ping.sendingTime, "A pong took too much time!")
      accumulator(remaining - 1, report.add(result))
    }

  accumulator(tries, ClockSynchronizationReport.empty) <* postedOnProgress(100)
}

def cancelGame(creds: GameUserCredentials): ZIO[HttpClient, ErrorADT, Unit] =
  (for {
    code <- postIgnore(Routes.inGameCancel, Routes.gameIdParam)(creds.gameId)
    _    <- unsuccessfulStatusCode(code)
  } yield ()).refineOrDie(ErrorADT.onlyErrorADT)
