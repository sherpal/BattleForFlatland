package websocketkeepers.gameantichamber

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.pattern.pipe
import dao.GameAntiChamberDAO
import errors.ErrorADT.GameDoesNotExist
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.gameantichamber.WebSocketProtocol.GameStatusUpdated
import models.bff.outofgame.MenuGame
import services.actors.ActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging._
import utils.ziohelpers.getOrFail
import zio.clock.Clock
import zio.{Has, ZIO, ZLayer}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

final class GameAntiChamber(
    gameId: String,
    layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
      ActorProvider.Service
    ]]
) extends Actor {

  import GameAntiChamber._
  import context.dispatcher

  def now: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

  def logInfo(message: String): Unit    = zio.Runtime.default.unsafeRun(log.info(message).provideLayer(layer))
  def logWarning(message: String): Unit = zio.Runtime.default.unsafeRun(log.warn(message).provideLayer(layer))

  /** Kicking all clients that don't give sign of live in the last specified time. */
  final val timeBeforeBeingKickedInSeconds = 30

  override def preStart(): Unit = {

    def fetchGameInfo(triesLeft: Int = 5): ZIO[GameTable, Throwable, Unit] =
      (for {
        maybeGame <- selectGameById(gameId)
        game <- getOrFail(maybeGame, GameDoesNotExist(gameId))
        _ <- ZIO.effectTotal(self ! game)
      } yield ()).catchSome {
        case _ if triesLeft > 0 => fetchGameInfo(triesLeft - 1)
      }

    zio.Runtime.default.unsafeRunToFuture(
      fetchGameInfo().catchAll(_ => ZIO.effectTotal(self ! CouldNotFetchGameInfo)).provideLayer(layer)
    )

    context.system.scheduler.scheduleAtFixedRate(
      timeBeforeBeingKickedInSeconds.seconds,
      timeBeforeBeingKickedInSeconds.seconds,
      self,
      CheckIfGameStillThere
    )
  }

  /**
    * Remembers the list of web socket clients (the player) that are connected to this game.
    */
  def receiver(players: Map[ActorRef, ClientInfo], menuGame: MenuGame, cancelling: Boolean = false): Actor.Receive = {
    val clients = players.keys

    {
      // here we handle messages from the frontend
      case msg: WebSocketProtocol =>
        msg match {
          case WebSocketProtocol.GameStatusUpdated =>
          case WebSocketProtocol.GameCancelled     =>
          case WebSocketProtocol.HeartBeat         =>
          case WebSocketProtocol.PlayerLeavesGame(userId) =>
            players.get(sender) match {
              case None =>
                logWarning("Received a PlayerLeavesGame message from someone unknown")
              case Some(ClientInfo(_, senderUserId, _)) if senderUserId == userId =>
                self ! PlayerLeavesGame(userId)
              case Some(ClientInfo(_, senderUserId, _)) =>
                logWarning(
                  s"Received a PlayerLeavesGame with a mismatch in user Id (Actual: $senderUserId, Received: $userId"
                )
            }
        }

      // below are all the internal stuff (backend)
      case JoinedGameDispatcher.SendHeartBeat =>
        // keeping connection alive
        clients.foreach(_ ! WebSocketProtocol.HeartBeat)
      case PlayerConnected(ref, userId) =>
        // a new player has connected. We tell it who we are, we notify all the others that a new client arrived
        // and we add it to the list
        context.watch(ref) ! Hello
        context.become(receiver(players + (ref -> ClientInfo(ref, userId, now)), menuGame))
        clients.foreach(_ ! GameStatusUpdated)
      case Terminated(ref) =>
        // a client is dead, perhaps because the client refreshed their pages or actually left
        // either way, we remove it from the set of players
        val newPlayersSet = players - ref
        context.become(receiver(newPlayersSet, menuGame))
      case YouCanClose =>
        // parent has done what needed to be done for me to close properly
        // if a PlayerConnected message happened to arrive in between (very unlikely!), we notify the parent that we
        // actually can't close
        if (players.isEmpty) {
          context.stop(self)
        } else {
          log.debug("Received a YouCanClose order but I'm not empty, crazy.")
          context.parent ! DidNotClose
        }
      case YouCanCleanUpCancel =>
        // parent is notified that the game is cancelled, and gave us the go to do so
        clients.foreach(_ ! WebSocketProtocol.GameCancelled)
        clients.foreach(context.unwatch)
        context.stop(self)
      case CancelGame if !cancelling =>
        // game creator has cancelled it (either willingly, or not)
        // we notify the parent that we need to cancel
        context.parent ! CancelGame
        context.become(receiver(players, menuGame, cancelling = true))
      case CancelGame =>
        log.debug("Already cancelling")
      case SeenAlive(userId) =>
        logInfo(s"SeenAlive: $userId")
        players.values.find(_.userId == userId).foreach { info =>
          context.become(
            receiver(
              players + (info.ref -> ClientInfo(info.ref, userId, now)),
              menuGame
            )
          )
        }
      case CheckAlive =>
        logInfo("About to kick people")
        zio.Runtime.default
          .unsafeRunToFuture(
            GameAntiChamberDAO
              .kickInactivePlayers(
                gameId,
                players.values.map { case ClientInfo(_, userId, lastTimeSeenAlive) => userId -> lastTimeSeenAlive }.toMap
              )
              .map {
                case (creatorWasKicked, peopleWereKicked) =>
                  if (creatorWasKicked) CancelGame
                  else if (peopleWereKicked) PeopleWereKicked
                  else akka.Done
              }
              .provideLayer(layer)
          )
          .pipeTo(self)
      case akka.Done =>
        logInfo("Nobody was kicked")
      case PeopleWereKicked =>
        logInfo("People were kicked, notifying remaining ones.")
        clients.foreach(_ ! GameStatusUpdated)

      case CheckIfGameStillThere =>
        zio.Runtime.default
          .unsafeRunToFuture(
            gameWithIdExists(gameId).map(if (_) CheckAlive else CancelGame).provideLayer(layer)
          )
          .pipeTo(self)

      case PlayerLeavesGame(userId) =>
        players.values.find(_.userId == userId).foreach {
          case ClientInfo(ref, _, _) =>
            zio.Runtime.default.unsafeRun(
              (removePlayerFromGame(userId, gameId) *> ZIO.effectTotal(clients.foreach(_ ! GameStatusUpdated)))
                .provideLayer(layer)
            )
            context.become(receiver(players - ref, menuGame))
        }

      case m =>
        logWarning(s"Received $m, weird")
    }
  }

  def waitingForGameInfo(stackedMessages: Queue[QueuedMessage]): Actor.Receive = {
    case CouldNotFetchGameInfo =>
      context.stop(self) // todo: do something
    case menuGame: MenuGame =>
      context.become(receiver(Map(), menuGame))
      stackedMessages.foreach {
        case QueuedMessage(message, ref) =>
          self.tell(message, ref)
      }
    case other =>
      context.become(waitingForGameInfo(stackedMessages.enqueue(QueuedMessage(other, sender))))
  }

  def receive: Actor.Receive = waitingForGameInfo(Queue())

}

object GameAntiChamber {

  sealed trait GameAntiChamberMessage
  case class QueuedMessage(message: Any, sender: ActorRef)
  case object CouldNotFetchGameInfo
  case class PlayerConnected(ref: ActorRef, userId: String) extends GameAntiChamberMessage
  case object IAmEmpty extends GameAntiChamberMessage
  case object YouCanClose extends GameAntiChamberMessage
  case object YouCanCleanUpCancel extends GameAntiChamberMessage
  case object DidNotClose extends GameAntiChamberMessage
  case object Hello extends GameAntiChamberMessage // notifying an AntiChamberClient that I take care of the game
  case object CancelGame extends GameAntiChamberMessage
  case object CheckIfGameStillThere extends GameAntiChamberMessage
  case object PeopleWereKicked extends GameAntiChamberMessage
  case class PlayerLeavesGame(userId: String) extends GameAntiChamberMessage

  /** Sent to this actor whenever a user pinged the server to say their are still connected. */
  case class SeenAlive(userId: String) extends GameAntiChamberMessage

  /** Sent from this actor to itself to tell it's time to kick inactive players. */
  private case object CheckAlive extends GameAntiChamberMessage

  private case class ClientInfo(ref: ActorRef, userId: String, lastTimeSeenAlive: LocalDateTime)

  def props(
      gameId: String,
      layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        ActorProvider.Service
      ]]
  ): Props = Props(new GameAntiChamber(gameId, layer))

}
