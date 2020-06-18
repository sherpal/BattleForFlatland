package services.gameserverlauncher

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import errors.ErrorADT.GameServerLauncherCouldNotBeReached
import models.bff.ingame.GameCredentials
import services.logging.{log, Logging}
import zio.{Has, Layer, UIO, ZIO, ZLayer}

object BGameServerLauncher {

  /**
    * This implementation of the [[services.gameserverlauncher.GameServerLauncher]] assumes that the
    * `game-server-launcher` node server is running in the background (or, equivalently, the `game-server-launcher.sc`
    * ammonite script).
    * When the game needs to be launched, a request is sent to the game server launcher server with the correct
    * arguments.
    */
  val usingLocalExternalNodeServer
      : ZLayer[Has[ActorSystem] with Logging with zio.clock.Clock, Nothing, Has[GameServerLauncher.Service]] =
    ZLayer.fromServices[ActorSystem, Logging.Service, zio.clock.Clock.Service, GameServerLauncher.Service] {
      (actorSystem: ActorSystem, logging: Logging.Service, clock: zio.clock.Clock.Service) =>
        implicit val as: ActorSystem = actorSystem
        (gameCredentials: GameCredentials) =>
          ZIO
            .fromFuture { implicit ec =>
              Http()
                .singleRequest(
                  HttpRequest(
                    HttpMethods.GET,
                    uri = Uri(
                      s"http://localhost:22223/run-game-server?" +
                        s"gameId=${gameCredentials.gameId}" +
                        s"&gameSecret=${gameCredentials.gameSecret}" +
                        s"&host=0.0.0.0"
                    )
                  )
                )
                .andThen {
                  case scala.util.Success(response: HttpResponse) => response.entity.discardBytes()
                }
                .filter(_.status.isSuccess)
            }
            .unit
            .timeout(zio.duration.Duration(1000, java.util.concurrent.TimeUnit.MILLISECONDS))
            .flatMap {
              case Some(success) => UIO(success)
              case None          => ZIO.fail(new Exception("Timeout"))
            }
            .tapError(error => log.error(error.getMessage))
            .orElseFail(GameServerLauncherCouldNotBeReached)
            .provideLayer(ZLayer.succeed(logging) ++ ZLayer.succeed(clock))
    }

  /**
    * This implementation of the [[services.gameserverlauncher.GameServerLauncher]] simply prints in the console the
    * sbt task to be ran.
    *
    * This implementation should be used as a very last resort while in development.
    */
  val usingManualLaunch: Layer[Nothing, Has[GameServerLauncher.Service]] =
    ZLayer.succeed(
      (gameCredentials: GameCredentials) =>
        ZIO.effectTotal {
          println(
            s"""
           |Could not reach game-server-launcher, fall back to manual launch:
           |Game secret for ${gameCredentials.gameId} is ${gameCredentials.gameSecret}.
           |Game server can be launched in sbt with the command:
           |game-server/run -i ${gameCredentials.gameId} -s ${gameCredentials.gameSecret}
           |""".stripMargin
          )
        }
    )

}
