package main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import errors.ErrorADT
import models.bff.ingame.InGameWSProtocol
import services.database.db
import services.database.gametables.GameTable
import slick.jdbc.PostgresProfile.api._
import zio.console._
import zio.{UIO, ZEnv, ZIO}

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
      val layer    = ZEnv.live ++ (db.Database.autoClosedDbProvider(dbObject) >>> GameTable.live)

      (for {
        _ <- putStrLn(s"Game server running for game ${config.gameId}")
        _ <- setup.fetchGameInfo(config.gameId).refineOrDie(ErrorADT.onlyErrorADT)
        actorSystem <- ZIO.effect(ActorSystem(server("localhost", 22222), "Server"))
        _ <- putStrLn("Press enter to close server...")
        _ <- getStrLn
        _ <- ZIO.effectTotal(actorSystem ! ServerBehavior.Stop)
      } yield 0)
        .catchAll(error => putStrLn(error.toString) *> UIO(1))
        .provideLayer(layer)
    case None => UIO(1)
  }

}
