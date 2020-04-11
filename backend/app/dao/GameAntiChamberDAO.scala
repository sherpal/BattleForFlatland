package dao

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import guards.Guards
import play.api.mvc.{AnyContent, Request}
import websocketkeepers.gameantichamber.{GameAntiChamber, JoinedGameDispatcher}
import zio.{Has, UIO, ZIO}
import akka.pattern.ask
import akka.util.Timeout
import errors.ErrorADT.GameHasBeenCancelled
import services.actors.ActorProvider.ActorProvider
import services.config.Configuration
import services.database.gametables.GameTable
import utils.playzio.HasRequest
import websocketkeepers.gameantichamber.JoinedGameDispatcher.{
  GameAntiChamberManagerFor,
  HereIsMaybeTheAntiChamberManagerFor
}
import services.logging._
import utils.ziohelpers.getOrFail
import zio.clock.Clock
import services.config._
import services.database.gametables._
import zio.clock._

import scala.concurrent.duration._

object GameAntiChamberDAO {

  def cancelGame(gameId: String): ZIO[Logging with ActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.headOfGame[AnyContent](gameId) // guarding
      _ <- deleteGame(request.gameInfo.game.gameName)
      maybeJoinedGameDispatcher <- services.actors.ActorProvider.actorRef(JoinedGameDispatcher.name)
      joinedGameDispatcher <- getOrFail(
        maybeJoinedGameDispatcher,
        new Exception("Joined game dispatcher does not exist, something is way off!")
      )
      maybeGameAntiChamberManagerRef <- ZIO
        .fromFuture { implicit ec =>
          implicit val timeout: Timeout = Timeout(1.second)
          (joinedGameDispatcher ? GameAntiChamberManagerFor(gameId)).mapTo[HereIsMaybeTheAntiChamberManagerFor]
        }
        .map(_.ref)
      gameAntiChamberManagerRef <- getOrFail(maybeGameAntiChamberManagerRef, GameHasBeenCancelled(gameId))
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamber.CancelGame)
      _ <- log.info(s"Game $gameId has been cancelled.")
    } yield ()

  def kickInactivePlayers(
      gameId: String,
      lastSeenAlive: Map[String, LocalDateTime]
  ): ZIO[GameTable with Logging with Clock with Configuration, Throwable, Boolean] =
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
    } yield creatorWasRemoved

}
