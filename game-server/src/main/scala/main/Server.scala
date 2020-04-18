package main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.HttpHeader
import errors.ErrorADT
import models.bff.ingame.{GameCredentials, InGameWSProtocol}
import services.database.db
import services.database.gametables.GameTable
import slick.jdbc.PostgresProfile.api._
import zio.console._
import zio.{Has, UIO, ZEnv, ZIO, ZLayer}
import io.circe.generic.auto._
import io.circe.syntax._

object Server extends zio.App {

  /** Echo server */
  private val server = new ServerBehavior[InGameWSProtocol, InGameWSProtocol] {
    def socketActor(outerWorld: ActorRef[InGameWSProtocol]): Behavior[InGameWSProtocol] =
      Behaviors.receiveMessage { message =>
        outerWorld ! message
        Behaviors.same
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
        actorSystem <- ZIO.access[Has[ActorSystem[ServerBehavior.ServerMessage]]](_.get)
        _ <- putStrLn("Press ctrl+c to close server...")
        _ <- setup.fetchGameInfo(credentials, actorSystem)
        //_ <- putStrLn(gameInfo.asJson.spaces2)
        _ <- getStrLn
        _ <- ZIO.effectTotal(actorSystem ! ServerBehavior.Stop)
      } yield 0)
        .catchAll(error => putStrLn(error.toString) *> UIO(1))
        .provideLayer(layer)
    case None => UIO(1)
  }

}
