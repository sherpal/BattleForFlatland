package main

import zio.{UIO, ZEnv, ZIO}
import zio.console._
import services.database.gametables.GameTable
import services.database.db
import akka.{actor, NotUsed}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.io.StdIn
import akka.actor.typed.scaladsl.adapter._
import errors.ErrorADT
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile.api._

object Server extends zio.App {

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = CLIConfig.makeConfig(args) match {
    case Some(config) =>
      val dbObject = Database.forConfig("slick.dbs.default.db")
      val layer    = ZEnv.live ++ (db.Database.dbProvider(dbObject) >>> GameTable.live)

      (for {
        _ <- putStrLn(s"Game server running for game ${config.gameId}")
        _ <- setup.fetchGameInfo(config.gameId).refineOrDie(ErrorADT.onlyErrorADT)
      } yield 0)
        .catchAll(error => putStrLn(error.toString) *> UIO(1))
        .provideLayer(layer)
    case None => UIO(1)
  }

}
