package dao

import guards.Guards
import play.api.mvc.{AnyContent, Request}
import websocketkeepers.gameantichamber.{GameAntiChamber, JoinedGameDispatcher}
import zio.{Has, ZIO}
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

import scala.concurrent.duration._

object GameAntiChamberDAO {

  def cancelGame(gameId: String): ZIO[Logging with ActorProvider with GameTable with Clock with Configuration with Has[
    HasRequest[Request, AnyContent]
  ], Throwable, Unit] =
    for {
      _ <- Guards.headOfGame[AnyContent](gameId) // guarding
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

}
