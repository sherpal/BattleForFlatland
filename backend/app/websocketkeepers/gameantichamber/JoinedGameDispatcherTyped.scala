package websocketkeepers.gameantichamber

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.bff.gameantichamber.WebSocketProtocol.GameCancelled
import models.users.User
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.ActorModule
import services.actors.TypedActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.{Logging, PlayLogging}
import slick.jdbc.JdbcProfile
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeperTyped
import zio.clock.Clock
import zio.{Has, ZLayer}

import scala.concurrent.duration._

object JoinedGameDispatcherTyped extends ActorModule {

  final val name = "JoinedGameDispatcherTyped"

  import GameAntiChamberTyped._

  sealed trait Message

  case class NewClient(gameId: String, user: User, ref: ActorRef[AntiChamberClientTyped.Message]) extends Message
  case class DidNotClose(ref: ActorRef[GameAntiChamberTyped.PlayerConnected]) extends Message
  case class CancelGame(ref: ActorRef[YouCanCleanUpCancel.type]) extends Message
  private case object SendHeartBeat extends Message
  private case class ChildTerminated(ref: ActorRef[_]) extends Message
  case class GameAntiChamberManagerFor(gameId: String, respondTo: ActorRef[HereIsMaybeTheAntiChamberManagerFor])
      extends Message

  @Provides
  def apply(
      dbConfigProvider: DatabaseConfigProvider,
      gameMenuBookKeeperRef: ActorRef[GameMenuRoomBookKeeperTyped.Message]
  ): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.withTimers { timerScheduler =>
      def db = dbConfigProvider.get[JdbcProfile].db

      val logger: Logger = Logger("GameAntiChamberActorSystem")

      val layer
          : ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
            TypedActorProvider.Service
          ]] =
        Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
          PlayLogging.live(logger) ++ TypedActorProvider.live(gameMenuBookKeeperRef, context.self)

      timerScheduler.startTimerAtFixedRate(SendHeartBeat, 5.seconds)

      behavior(BehaviorInfo(Map(), layer))
    }
  }

  def behavior(info: BehaviorInfo): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case NewClient(gameId, user, ref) =>
        info.gameActors.get(gameId) match {
          case Some(info)
              if info.gameIsCancelling || info.isClosing => // game already cancelling, we directly notify the client
            ref ! AntiChamberClientTyped
              .WebSocketProtocolWrapper(GameCancelled, AntiChamberClientTyped.GameAntiChamberSender)
            Behaviors.same
          case Some(info)
              if !info.isClosing => // this game is already taken care of, and not closing, we notify the actor
            info.ref ! GameAntiChamberTyped.PlayerConnected(ref, user)
            Behaviors.same
          case None => // this game does not exist, we create it and start over
            context.self ! NewClient(gameId, user, ref)
            val antiChamber = context.spawn(
              GameAntiChamberTyped(gameId, context.self, info.layer),
              "AntiChamber-" + gameId.filterNot(_ == '-')
            )
            context.watchWith(antiChamber, ChildTerminated(antiChamber))
            behavior(info.copy(gameActors = info.gameActors + (gameId -> GameAntiChamberInfo(gameId, antiChamber))))
        }
      case DidNotClose(ref) =>
        // the child did not close because by the time it received the YouCanClose message,
        // it actually received other PlayerConnected Message.
        // We send it all the messages that we stacked until then and clear the stack, putting its status
        // as open.
        info.gameActors
          .find(_._2.ref == ref)
          .map {
            case (gameId, gameInfo) =>
              gameInfo.stackedPlayerConnected.foreach(ref ! _)
              behavior(info.copy(gameActors = info.gameActors + (gameId -> gameInfo.clearStack.openMode)))
          }
          .getOrElse(Behaviors.same)
      case CancelGame(ref) =>
        // the game has been cancelled. We notify the sender that it can close all connections then die, and we put it
        // into cancel mode, and we notify all the stacked messages that the game has been cancelled
        info.gameActors
          .find(_._2.ref.narrow[GameAntiChamberTyped.YouCanCleanUpCancel.type] == ref)
          .map {
            case (gameId, gameInfo) =>
              gameInfo.stackedPlayerConnected
                .map(_.ref)
                .foreach(
                  _ ! AntiChamberClientTyped
                    .WebSocketProtocolWrapper(GameCancelled, AntiChamberClientTyped.GameAntiChamberSender)
                )
              ref ! YouCanCleanUpCancel
              behavior(info.copy(gameActors = info.gameActors + (gameId -> gameInfo.cancelling.clearStack)))
          }
          .getOrElse(Behaviors.same)
      case SendHeartBeat =>
        info.gameActors.values.map(_.ref).foreach(_ ! GameAntiChamberTyped.SendHeartBeat)
        Behaviors.same
      case ChildTerminated(ref) =>
        // The child actually was killed. If we still have stacked messages, we create a new one and send it all the
        // stacked messages.
        info.gameActors
          .find(_._2.ref == ref)
          .map {
            case (gameId, gameInfo) =>
              if (gameInfo.stackedPlayerConnected.isEmpty) behavior(info.copy(gameActors = info.gameActors - gameId))
              else {
                val newChild = context.spawn(
                  GameAntiChamberTyped(gameId, context.self, info.layer),
                  "AntiChamber-" + gameId.filterNot(_ == '-')
                )
                context.watch(newChild)
                behavior(info.copy(gameActors = info.gameActors + (gameId -> GameAntiChamberInfo(gameId, newChild))))
              }
          }
          .getOrElse(Behaviors.same)
      case GameAntiChamberManagerFor(gameId, respondTo) =>
        respondTo ! HereIsMaybeTheAntiChamberManagerFor(
          gameId,
          info.gameActors.get(gameId).filterNot(_.gameIsCancelling).filterNot(_.isClosing).map(_.ref)
        )
        Behaviors.same
    }
  }

  private case class GameAntiChamberInfo(
      gameId: String,
      ref: ActorRef[GameAntiChamberTyped.MessageFromOutside],
      stackedPlayerConnected: Set[GameAntiChamberTyped.PlayerConnected] = Set(),
      isClosing: Boolean                                                = false,
      gameIsCancelling: Boolean                                         = false
  ) {
//    def stackMessage(playerConnected: GameAntiChamberTyped.PlayerConnected): GameAntiChamberInfo =
//      copy(stackedPlayerConnected = stackedPlayerConnected + playerConnected)
//
//    def closingMode: GameAntiChamberInfo = copy(isClosing = true)
    def openMode: GameAntiChamberInfo = copy(isClosing = false)

    def cancelling: GameAntiChamberInfo = copy(gameIsCancelling = true)

    def clearStack: GameAntiChamberInfo = copy(stackedPlayerConnected = Set())
  }

  /**
    * The following class stores all information the behavior needs to now at any given time.
    * This is a short-cut for not having to write all arguments all the time.
    */
  private case class BehaviorInfo(
      gameActors: Map[String, GameAntiChamberInfo],
      layer: ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        TypedActorProvider.Service
      ]]
  )

  case class HereIsMaybeTheAntiChamberManagerFor(
      gameId: String,
      ref: Option[ActorRef[GameAntiChamberTyped.MessageFromOutside]]
  )

}
