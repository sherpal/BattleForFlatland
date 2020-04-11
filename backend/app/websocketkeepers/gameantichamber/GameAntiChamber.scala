package websocketkeepers.gameantichamber

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.pattern.pipe
import dao.GameAntiChamberDAO
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.gameantichamber.WebSocketProtocol.GameStatusUpdated
import services.actors.ActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging._
import zio.clock.Clock
import zio.{Has, ZLayer}

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

  def logInfo(message: String): Unit = zio.Runtime.default.unsafeRun(log.info(message).provideLayer(layer))

  /** Kicking all clients that don't give sign of live in the last specified time. */
  final val timeBeforeBeingKickedInSeconds = 30

  override def preStart(): Unit =
    context.system.scheduler.scheduleAtFixedRate(
      timeBeforeBeingKickedInSeconds.seconds,
      timeBeforeBeingKickedInSeconds.seconds,
      self,
      CheckIfGameStillThere
    )

  /**
    * Remembers the list of web socket clients (the player) that are connected to this game.
    */
  def receiver(players: Map[ActorRef, ClientInfo], cancelling: Boolean = false): Actor.Receive = {
    val clients = players.keys

    {
      case JoinedGameDispatcher.SendHeartBeat =>
        // keeping connection alive
        clients.foreach(_ ! WebSocketProtocol.HeartBeat)
      case PlayerConnected(ref, userId) =>
        // a new player has connected. We tell it who we are, we notify all the others that a new client arrived
        // and we add it to the list
        context.watch(ref) ! Hello
        context.become(receiver(players + (ref -> ClientInfo(ref, userId, now))))
        clients.foreach(_ ! GameStatusUpdated)
      case Terminated(ref) =>
        // a client is dead, perhaps because the client refreshed their pages or actually left
        // either way, we remove it from the set of players
        val newPlayersSet = players - ref
        context.become(receiver(newPlayersSet))
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
        context.become(receiver(players, cancelling = true))
      case CancelGame =>
        log.debug("Already cancelling")
      case SeenAlive(userId) =>
        logInfo(s"SeenAlive: $userId")
        players.values.find(_.userId == userId).foreach { info =>
          context.become(
            receiver(
              players + (info.ref -> ClientInfo(info.ref, userId, now))
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
            context.become(receiver(players - ref))
            clients.foreach(_ ! GameStatusUpdated)
        }

    }
  }

  def receive: Actor.Receive = receiver(Map())

}

object GameAntiChamber {

  sealed trait GameAntiChamberMessage
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
