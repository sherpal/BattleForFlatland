package main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import game.ActionUpdateCollector
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.ingame.InGameWSProtocol.{Ping, Pong}
import models.bff.ingame.{GameCredentials, InGameWSProtocol}
import services.database.db
import services.database.gametables.GameTable
import slick.jdbc.PostgresProfile.api._
import zio.console._
import zio.{Has, UIO, ZEnv, ZIO}

import scala.concurrent.duration._

object Server extends zio.App {

  /** Echo server */
  private val server = new ServerBehavior[InGameWSProtocol, InGameWSProtocol] {
    def socketActor(
        outerWorld: ActorRef[InGameWSProtocol],
        actionUpdateCollector: ActorRef[ActionUpdateCollector.Message]
    ): Behavior[InGameWSProtocol] =
      Behaviors.setup { context =>
        actionUpdateCollector ! ActionUpdateCollector.NewExternalMember(context.self)

        Behaviors.withTimers { timerScheduler =>
          timerScheduler.startTimerAtFixedRate(InGameWSProtocol.HeartBeat, 5.seconds)

          Behaviors.receiveMessage {
            case Ping(sendingTime) =>
              outerWorld ! Pong(sendingTime, System.currentTimeMillis)
              Behaviors.same
            case message: InGameWSProtocol.Incoming => // incoming messages are sent to the frontend
              outerWorld ! message
              Behaviors.same
            case message: InGameWSProtocol.Outgoing => // outgoing messages should be forwarded to the inner actors
              println(s"Message comes from the frontend and should be handled: $message")
              Behaviors.unhandled
          }
        }
      }
  }

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = CLIConfig.makeConfig(args) match {
    case Some(config) =>
      val dbObject = Database.forConfig("slick.dbs.default.db")

      val layer = ZEnv.live ++ (db.Database.autoClosedDbProvider(dbObject) >>> GameTable.live) ++
        server.launchServer(config.host, config.port)

      (for {
        _ <- putStrLn(s"Game server running for game ${config.gameId}")
        credentials <- UIO(GameCredentials(config.gameId, config.gameSecret))
        actorSystem <- ZIO
          .access[Has[ActorSystem[ServerBehavior.ServerMessage]]](_.get[ActorSystem[ServerBehavior.ServerMessage]])
        _ <- putStrLn("""Execute curl -X GET "http://localhost:22222/stop" to close the server.""")
        allGameInfo <- setup.fetchGameInfo(credentials, actorSystem)
        _ <- putStrLn(allGameInfo.asJson.spaces2)
        _ <- ZIO.effectTotal(
          actorSystem ! ServerBehavior.ReceivedCredentials(
            allGameInfo.gameInfo.players,
            allGameInfo.allGameCredentials.allGameUserCredentials
          )
        )
        _ <- server.waitForServerToStop(actorSystem)
      } yield 0)
        .catchAll(error => putStrLn(error.toString) *> UIO(1))
        .provideLayer(layer)
    case None => UIO(1)
  }

}
