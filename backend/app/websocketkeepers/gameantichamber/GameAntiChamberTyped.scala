package websocketkeepers.gameantichamber

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import dao.GameAntiChamberDAO
import errors.ErrorADT.GameDoesNotExist
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.gameantichamber.WebSocketProtocol.{GameCancelled, GameStatusUpdated}
import models.bff.outofgame.MenuGame
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.gametables._
import services.logging.{log, Logging}
import utils.ziohelpers.getOrFail
import websocketkeepers.gameantichamber.JoinedGameDispatcherTyped.DidNotClose
import zio.clock.Clock
import zio.{Has, UIO, ZIO, ZLayer}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

object GameAntiChamberTyped {

  private val timeBeforeBeingKickedInSeconds = 30
  def now: LocalDateTime                     = LocalDateTime.now(ZoneOffset.UTC)

  sealed trait Message

  sealed trait MessageWaitingGameInfo extends Message
  case object CouldNotFetchGameInfo extends MessageWaitingGameInfo
  case class MenuGameWrapper(menuGame: MenuGame) extends MessageWaitingGameInfo

  case object CheckIfGameStillThere extends Message
  case class WebSocketProtocolWrapper(
      protocol: WebSocketProtocol,
      sender: ActorRef[AntiChamberClientTyped.WebSocketProtocolWrapper]
  ) extends Message

  private case class PlayerDisconnected(ref: ActorRef[AntiChamberClientTyped.WebSocketProtocolWrapper]) extends Message
  case class YouCanClose(ref: ActorRef[JoinedGameDispatcherTyped.DidNotClose]) extends Message
  private case object CheckAlive extends Message
  private case object PeopleWereKicked extends Message
  private case object Dummy extends Message

  sealed trait MessageFromOutside extends Message
  case class PlayerConnected(ref: ActorRef[AntiChamberClientTyped.Message], userId: String) extends MessageFromOutside
  case class SeenAlive(userId: String) extends MessageFromOutside
  case class PlayerLeavesGame(userId: String) extends MessageFromOutside
  case object SendHeartBeat extends MessageFromOutside
  case object YouCanCleanUpCancel extends MessageFromOutside
  case object CancelGame extends MessageFromOutside

  private case class ClientInfo(ref: ActorRef[Nothing], userId: String, lastTimeSeenAlive: LocalDateTime)

  private def protocol(protocolMessage: WebSocketProtocol) =
    AntiChamberClientTyped.WebSocketProtocolWrapper(protocolMessage, AntiChamberClientTyped.GameAntiChamberSender)

  def apply(
      gameId: String,
      parent: ActorRef[JoinedGameDispatcherTyped.CancelGame],
      layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        TypedActorProvider.Service
      ]]
  ): Behavior[Message] = waitingForGameInfo(Queue(), gameId, parent, layer)

  def behavior(
      menuGame: MenuGame,
      players: Map[ActorRef[AntiChamberClientTyped.WebSocketProtocolWrapper], ClientInfo],
      parent: ActorRef[JoinedGameDispatcherTyped.CancelGame],
      layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        TypedActorProvider.Service
      ]],
      cancelling: Boolean = false
  ): Behavior[Message] = {
    val clients = players.keys

    def logInfo(message: String): Unit = zio.Runtime.default.unsafeRun(log.info(message).provideLayer(layer))
    //def logWarning(message: String): Unit = zio.Runtime.default.unsafeRun(log.warn(message).provideLayer(layer))

    Behaviors.receive { (context, message) =>
      message match {
        case _: MessageWaitingGameInfo => Behaviors.unhandled
        case WebSocketProtocolWrapper(protocol, sender) =>
          protocol match {
            case WebSocketProtocol.GameStatusUpdated => Behaviors.same
            case WebSocketProtocol.GameCancelled     => Behaviors.same
            case WebSocketProtocol.HeartBeat         => Behaviors.same
            case WebSocketProtocol.PlayerLeavesGame(userId) =>
              players.get(sender) match {
                case None => Behaviors.same
                case Some(ClientInfo(_, senderUserId, _)) if senderUserId == userId =>
                  context.self ! PlayerLeavesGame(userId)
                  Behaviors.same
                case _ => Behaviors.same
              }
          }
        case SendHeartBeat =>
          // keeping connection alive
          clients.foreach(_ ! protocol(WebSocketProtocol.HeartBeat))
          Behaviors.same
        case PlayerConnected(ref, userId) =>
          // a new player has connected. We tell it who we are, we notify all the others that a new client arrived
          // and we add it to the list
          context.watchWith(ref, PlayerDisconnected(ref))
          ref ! AntiChamberClientTyped.HelloFromAntiChamber(context.self)
          clients.foreach(_ ! protocol(GameStatusUpdated))
          behavior(menuGame, players + (ref -> ClientInfo(ref, userId, now)), parent, layer)
        case PlayerDisconnected(ref) =>
          behavior(menuGame, players - ref, parent, layer)
        case YouCanClose(ref) =>
          // parent has done what needed to be done for me to close properly
          // if a PlayerConnected message happened to arrive in between (very unlikely!), we notify the parent that we
          // actually can't close
          if (players.isEmpty) Behaviors.stopped
          else {
            ref ! DidNotClose(context.self.narrow[PlayerConnected])
            Behaviors.same
          }
        case YouCanCleanUpCancel =>
          // parent is notified that the game is cancelled, and gave us the go to do so
          clients.foreach(_ ! protocol(GameCancelled))
          clients.foreach(context.unwatch)
          Behaviors.stopped
        case CancelGame if !cancelling =>
          parent ! JoinedGameDispatcherTyped.CancelGame(context.self)
          behavior(menuGame, players, parent, layer, cancelling = true)
        case CancelGame =>
          context.log.debug("Already cancelling")
          Behaviors.same
        case SeenAlive(userId) =>
          players
            .find(_._2.userId == userId)
            .map {
              case (ref, info) =>
                behavior(menuGame, players + (ref -> ClientInfo(info.ref, userId, now)), parent, layer)
            }
            .getOrElse(Behaviors.same)
        case CheckAlive =>
          logInfo("About to kick people")

          context.pipeToSelf(
            zio.Runtime.default
              .unsafeRunToFuture(
                GameAntiChamberDAO
                  .kickInactivePlayers(
                    menuGame.gameId,
                    players.values.map { case ClientInfo(_, userId, lastTimeSeenAlive) => userId -> lastTimeSeenAlive }.toMap
                  )
                  .map {
                    case (creatorWasKicked, peopleWereKicked) =>
                      if (creatorWasKicked) CancelGame
                      else if (peopleWereKicked) PeopleWereKicked
                      else Dummy
                  }
                  .provideLayer(layer)
              )
          ) {
            case scala.util.Success(message) => message
            case scala.util.Failure(ex)      => throw ex // can't happen
          }

          Behaviors.same
        case Dummy =>
          logInfo("Nobody was kicked")
          Behaviors.same
        case PeopleWereKicked =>
          logInfo("People were Kicked, notifying remaining ones.")
          Behaviors.same
        case CheckIfGameStillThere =>
          context.pipeToSelf(
            zio.Runtime.default.unsafeRunToFuture(
              gameWithIdExists(menuGame.gameId).map(if (_) CheckAlive else CancelGame).provideLayer(layer)
            )
          ) {
            case scala.util.Success(message) => message
            case scala.util.Failure(ex)      => throw ex
          }
          Behaviors.same
        case PlayerLeavesGame(userId) =>
          players
            .find(_._2.userId == userId)
            .map {
              case (ref, _) =>
                zio.Runtime.default.unsafeRun(
                  (removePlayerFromGame(userId, menuGame.gameId) *> ZIO
                    .effectTotal(clients.foreach(_ ! protocol(GameStatusUpdated))))
                    .provideLayer(layer)
                )
                behavior(menuGame, players - ref, parent, layer)
            }
            .getOrElse(Behaviors.same)
      }
    }
  }

  private def fetchGameInfo(gameId: String, triesLeft: Int = 5): ZIO[GameTable, Throwable, MessageWaitingGameInfo] =
    (for {
      maybeGame <- selectGameById(gameId)
      game <- getOrFail(maybeGame, GameDoesNotExist(gameId))
    } yield MenuGameWrapper(game)).catchSome {
      case _ if triesLeft > 0 => fetchGameInfo(gameId, triesLeft - 1)
    }

  private def waitingForGameInfo(
      stackedMessages: Queue[Message],
      gameId: String,
      parent: ActorRef[JoinedGameDispatcherTyped.CancelGame],
      layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        TypedActorProvider.Service
      ]]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timerScheduler =>
        context.pipeToSelf(
          zio.Runtime.default.unsafeRunToFuture(
            fetchGameInfo(gameId).catchAll(_ => UIO(CouldNotFetchGameInfo)).provideLayer(layer)
          )
        ) {
          case scala.util.Success(message) => message
          case scala.util.Failure(ex)      => throw ex // can't happen
        }

        timerScheduler.startTimerAtFixedRate(CheckIfGameStillThere, timeBeforeBeingKickedInSeconds.seconds)

        Behaviors.receiveMessage {
          case message: MessageWaitingGameInfo =>
            message match {
              case CouldNotFetchGameInfo =>
                Behaviors.stopped
              case MenuGameWrapper(menuGame) =>
                stackedMessages.foreach(context.self ! _)
                behavior(menuGame, Map(), parent, layer)
            }
          case other =>
            waitingForGameInfo(stackedMessages.enqueue(other), gameId, parent, layer)
        }
      }
    }
}
