package dao

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.util.Timeout
import errors.ErrorADT.GameHasBeenCancelled
import guards.Guards
import play.api.mvc.{AnyContent, Request}
import services.actors.TypedActorProvider._
import services.config.{Configuration, _}
import services.crypto.Crypto
import services.database.gamecredentials._
import services.database.gametables.{GameTable, _}
import services.gameserverlauncher._
import services.logging._
import utils.playzio.HasRequest
import utils.ziohelpers.getOrFail
import websocketkeepers.gameantichamber.GameAntiChamberTyped
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped.{
  GameAntiChamberManagerFor,
  HereIsMaybeTheAntiChamberManagerFor
}
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.clock.{Clock, _}
import zio.{Has, UIO, ZIO}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object GameAntiChamberDAO {

  private[dao] def askGameAntiChamberManager(gameId: String)(implicit scheduler: Scheduler) =
    for {
      dispatcherRef <- joinedGameDispatcherRef
      maybeGameAntiChamberManagerRef <- ZIO
        .fromFuture { implicit ec =>
          implicit val timeout: Timeout = Timeout(1.second)
          dispatcherRef.ask[HereIsMaybeTheAntiChamberManagerFor](replyTo => GameAntiChamberManagerFor(gameId, replyTo))
        }
        .map(_.ref)
      gameAntiChamberManagerRef <- getOrFail(maybeGameAntiChamberManagerRef, GameHasBeenCancelled(gameId))
    } yield gameAntiChamberManagerRef

  def cancelGame(gameId: String)(
      implicit scheduler: Scheduler
  ): ZIO[Logging with TypedActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.headOfGame[AnyContent](gameId) // guarding
      _ <- deleteGame(request.gameInfo.game.gameName)
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamberTyped.CancelGame)
      bookKeeper <- gameMenuRoomBookKeeperRef
      _ <- ZIO.effectTotal(bookKeeper ! GameMenuRoomBookKeeperTyped.GameListUpdate)
      _ <- log.info(s"Game $gameId has been cancelled.")
    } yield ()

  /**
    * When the game creator asks to launch the game, we do the following:
    * - verify that it's the game creator who asked
    * - fetch game information from database
    * - create game credentials and add them to database
    * - try to contact the game server launcher to launch the game.
    * - if the previous fails, we display the sbt command to execute in order to launch the game (this will change in
    * production).
    */
  def startGame(
      gameId: String
  )(
      implicit actorSystem: ActorSystem
  ): ZIO[
    GameServerLauncher with Logging with GameCredentialsDB with Crypto with GameTable with Clock with Configuration with Has[
      HasRequest[Request, AnyContent]
    ],
    Throwable,
    Unit
  ] =
    for {
      _ <- Guards.headOfGame[AnyContent](gameId) // guarding
      gameInfo <- gameWithPlayersById(gameId)
      _ <- removeAllGameCredentials(gameId)
      gameCredentials <- createAndAddGameCredentials(gameInfo)
      _ <- launchGame(gameCredentials.gameCredentials)
    } yield ()

  def iAmStillThere(
      gameId: String
  )(implicit scheduler: Scheduler): ZIO[TypedActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.partOfGame[AnyContent](gameId) // guarding
      user = request.user
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamberTyped.SeenAlive(user.userId))
    } yield ()

  def leaveGame(
      gameId: String
  )(implicit scheduler: Scheduler): ZIO[TypedActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.partOfGame[AnyContent](gameId)
      user = request.user
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamberTyped.PlayerLeavesGame(user.userId))
      _ <- removePlayerFromGame(user.userId, gameId)
    } yield ()

  def kickInactivePlayers(
      gameId: String,
      lastSeenAlive: Map[String, LocalDateTime]
  ): ZIO[GameTable with Logging with Clock with Configuration, Throwable, (Boolean, Boolean)] =
    for {
      idleTime <- timeBeforePlayersGetKickedInSeconds
      gameInfo <- gameWithPlayersById(gameId)
      nowSeconds <- currentTime(TimeUnit.SECONDS)
      now = LocalDateTime.ofEpochSecond(nowSeconds, 0, ZoneOffset.UTC)
      playersToKick = gameInfo.players.filter { user =>
        !lastSeenAlive.get(user.userId).exists(_.until(now, ChronoUnit.SECONDS) < idleTime)
      }
      _ <- if (playersToKick.nonEmpty)
        log.info(s"The following players were inactive: ${playersToKick.map(_.userName).mkString(", ")}")
      else UIO(())
      creatorWasRemoved <- ZIO
        .foreachParN(4)(playersToKick) { user =>
          removePlayerFromGame(user.userId, gameId)
        }
        .map(_.exists(identity))
    } yield (creatorWasRemoved, playersToKick.nonEmpty) // returning if the creator was removed and if people where kicked

}
