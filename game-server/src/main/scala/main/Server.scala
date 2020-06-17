package main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import game.{ActionTranslator, AntiChamber}
import io.circe.generic.auto._
import io.circe.syntax._
import models.bff.ingame.InGameWSProtocol.{GameActionWrapper, LetsBegin, Ping, Pong, Ready, ReadyToStart}
import models.bff.ingame.{GameCredentials, InGameWSProtocol}
import services.database.db
import services.database.gametables.GameTable
import slick.jdbc.PostgresProfile.api._
import zio.console._
import zio.{UIO, ZEnv, ZIO}
import communication.BFFPicklers._

import scala.concurrent.duration._

object Server extends zio.App {

  /** Echo server */
  private val server = new ServerBehavior[InGameWSProtocol, InGameWSProtocol] {
    def socketActor(
        outerWorld: ActorRef[Either[InGameWSProtocol, InGameWSProtocol]],
        antiChamber: ActorRef[AntiChamber.Message],
        actionTranslator: ActorRef[ActionTranslator.Message]
    ): Behavior[InGameWSProtocol] =
      Behaviors.setup { context =>
        Behaviors.withTimers { timerScheduler =>
          timerScheduler.startTimerAtFixedRate(InGameWSProtocol.HeartBeat, 5.seconds)

          Behaviors.receiveMessage {
            case Ping(sendingTime) =>
              outerWorld ! Left(Pong(sendingTime, System.currentTimeMillis))
              Behaviors.same
            case Ready(userId) =>
              antiChamber ! AntiChamber.Ready(userId, context.self)
              Behaviors.same
            case ReadyToStart(userId) =>
              antiChamber ! AntiChamber.ReadyToStart(userId)
              Behaviors.same
            case LetsBegin =>
              antiChamber ! AntiChamber.LetsBegin
              Behaviors.same
            case actionWrapper: GameActionWrapper =>
              actionTranslator ! ActionTranslator.InGameWSProtocolWrapper(actionWrapper)
              Behaviors.same
            case message: InGameWSProtocol.Incoming => // incoming messages are sent to the frontend
              outerWorld ! Left(message)
              Behaviors.same
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
        actorSystem <- ZIO.service[ActorSystem[ServerBehavior.ServerMessage]]
        _ <- putStrLn("""Execute curl -X POST "http://localhost:22222/stop" to close the server.""")
        allGameInfo <- setup.fetchGameInfo(credentials, actorSystem)
        _ <- putStrLn(allGameInfo.asJson.spaces2)
        _ <- ZIO.effectTotal(
          actorSystem ! ServerBehavior.ReceivedCredentials(
            allGameInfo.gameInfo.players,
            allGameInfo.allGameCredentials.allGameUserCredentials,
            allGameInfo.gameInfo
          )
        )
        _ <- server.waitForServerToStop(actorSystem)
      } yield 0)
        .catchAll(error => putStrLn(error.toString) *> UIO(1))
        .provideLayer(layer)
    case None => UIO(1)
  }

}
