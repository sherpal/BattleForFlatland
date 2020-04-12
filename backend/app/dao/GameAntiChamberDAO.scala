package dao

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import errors.ErrorADT.GameHasBeenCancelled
import guards.Guards
import play.api.mvc.{AnyContent, Request}
import services.actors.ActorProvider.ActorProvider
import services.config.{Configuration, _}
import services.database.gametables.{GameTable, _}
import services.logging._
import utils.playzio.HasRequest
import utils.ziohelpers.getOrFail
import websocketkeepers.gameantichamber.JoinedGameDispatcher.{
  GameAntiChamberManagerFor,
  HereIsMaybeTheAntiChamberManagerFor
}
import websocketkeepers.gameantichamber.{GameAntiChamber, JoinedGameDispatcher}
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper
import zio.clock.{Clock, _}
import zio.{Has, UIO, ZIO}

import scala.concurrent.duration._

object GameAntiChamberDAO {

  private def askGameAntiChamberManager(gameId: String) =
    for {
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
    } yield gameAntiChamberManagerRef

  private val askGameMenuRoomBookKeeper = for {
    maybeBookKeeper <- services.actors.ActorProvider.actorRef(GameMenuRoomBookKeeper.name)
    bookKeeper <- getOrFail(
      maybeBookKeeper,
      new Exception("GameMenuRoomBookKeeper actor is not there, something is way off!")
    )
  } yield bookKeeper

  def cancelGame(gameId: String): ZIO[Logging with ActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.headOfGame[AnyContent](gameId) // guarding
      _ <- deleteGame(request.gameInfo.game.gameName)
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamber.CancelGame)
      bookKeeper <- askGameMenuRoomBookKeeper
      _ <- ZIO.effectTotal(bookKeeper ! GameMenuRoomBookKeeper.GameListUpdate)
      _ <- log.info(s"Game $gameId has been cancelled.")
    } yield ()

  def iAmStillThere(gameId: String): ZIO[ActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.partOfGame[AnyContent](gameId) // guarding
      user = request.user
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamber.SeenAlive(user.userId))
    } yield ()

  def leaveGame(gameId: String): ZIO[ActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      request <- Guards.partOfGame[AnyContent](gameId)
      user = request.user
      gameAntiChamberManagerRef <- askGameAntiChamberManager(gameId)
      _ <- ZIO.effectTotal(gameAntiChamberManagerRef ! GameAntiChamber.PlayerLeavesGame(user.userId))
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
