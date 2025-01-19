package services.errorreporting

import zio.ZIO
import zio.ZLayer

trait ErrorReporting {

  def showError(err: Throwable): ZIO[Any, Nothing, Unit]

}

object ErrorReporting {

  val live = ZLayer.fromZIO(for {
    errorReporting <- ZIO.succeed(FErrorReporting("error-reporting-container"))
  } yield (errorReporting: ErrorReporting))

}
