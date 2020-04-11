package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorRef, Terminated}
import javax.inject.{Inject, Singleton}
import models.bff.gameantichamber.WebSocketProtocol
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import services.actors.ActorProvider
import services.config.Configuration
import services.crypto.Crypto
import services.database.db.Database.dbProvider
import services.database.gametables.GameTable
import services.logging.{Logging, PlayLogging}
import slick.jdbc.JdbcProfile
import zio.clock.Clock
import zio.{Has, ZLayer}

import scala.concurrent.duration._

/**
  * The role of the [[JoinedGameDispatcher]] is to listen to WebSocket connections
  */
@Singleton
final class JoinedGameDispatcher @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends Actor
    with HasDatabaseConfigProvider[JdbcProfile] {

  lazy val logger: Logger = Logger("GameAntiChamberActorSystem")

  val layer
      : ZLayer[Any, Nothing, Clock with Configuration with GameTable with Crypto with Has[Logging.Service] with Has[
        ActorProvider.Service
      ]] = Clock.live ++ Configuration.live ++ (dbProvider(db) >>> GameTable.live) ++ Crypto.live ++
    PlayLogging.live(logger) ++ ActorProvider.live(Map(JoinedGameDispatcher.name -> self))

  import context.dispatcher
  import websocketkeepers.gameantichamber.JoinedGameDispatcher._

  override def preStart(): Unit =
    context.system.scheduler.scheduleAtFixedRate(5.seconds, 5.seconds, self, SendHeartBeat)

  /**
    * Map from the game id to the [[akka.actor.ActorRef]] of the actor in charge of that game, together with the
    * pending messages that this actor is supposed to receive, and whether we are on a "waiting" state.
    *
    * When a child is in waiting state, we stack the new messages for it in the Set of
    * [[GameAntiChamber.PlayerConnected]] messages. When a child says he's empty, we put it in waiting state and tell
    * it that it can die.
    * When a child die, if it still had pending messages, we re-create a new one and we flush the set.
    *
    */
  def receiver(gameActors: Map[String, GameAntiChamberInfo]): Actor.Receive = {
    case SendHeartBeat =>
      gameActors.values.map(_.ref).foreach(_ ! SendHeartBeat) // keeping connections alive
    case NewClient(gameId, userId) => // a new web socket client arrives
      gameActors.get(gameId) match {
        case Some(info) if info.gameIsCancelling => // game already cancelling, we directly notify the client
          sender ! WebSocketProtocol.GameCancelled
        case Some(info)
            if !info.isClosing => // this game is already taken care of, and not closing, we notify the actor
          info.ref ! GameAntiChamber.PlayerConnected(sender, userId)
        case Some(info) if info.isClosing => // this game already exists, but closing, we stack the message
          context.become(
            receiver(gameActors + (gameId -> info.stackMessage(GameAntiChamber.PlayerConnected(sender, userId))))
          )
        case None => // this game does not exist, we create it and start over
          context.become(
            receiver(
              gameActors + (gameId -> GameAntiChamberInfo(
                gameId,
                context.watch(context.actorOf(GameAntiChamber.props(gameId, layer)))
              ))
            )
          )
          self forward NewClient(gameId, userId)
      }
    case GameAntiChamber.IAmEmpty => // a child has no more connection. We tell it to close and put it in closing mode
      sender ! GameAntiChamber.YouCanClose
      gameActors.find(_._2.ref == sender).foreach {
        case (gameId, gameInfo) =>
          context.become(
            receiver(gameActors + (gameId -> gameInfo.closingMode))
          )
      }
    case GameAntiChamber.DidNotClose =>
      // the child did not close because by the time it received the YouCanClose message,
      // it actually received other PlayerConnected Message.
      // We send it all the messages that we stacked until then and clear the stack, putting its status
      // as open.
      gameActors.find(_._2.ref == sender).foreach {
        case (gameId, gameInfo) =>
          gameInfo.stackedPlayerConnected.foreach(sender ! _)
          context.become(
            receiver(
              gameActors + (gameId -> gameInfo.clearStack.openMode)
            )
          )
      }
    case GameAntiChamber.CancelGame =>
      // the game has been cancelled. We notify the sender that it can close all connections then die, and we put it
      // into cancel mode, and we notify all the stacked messages that the game has been cancelled
      gameActors.find(_._2.ref == sender).foreach {
        case (gameId, gameInfo) =>
          gameInfo.stackedPlayerConnected.map(_.ref).foreach(_ ! WebSocketProtocol.GameCancelled)
          sender ! GameAntiChamber.YouCanCleanUpCancel
          context.become(receiver(gameActors + (gameId -> gameInfo.cancelling.clearStack)))

      }
    case Terminated(child) =>
      // The child actually was killed. If we still have stacked messages, we create a new one and send it all the
      // stacked messages.
      gameActors.find(_._2.ref == child).foreach {
        case (gameId, gameInfo) =>
          if (gameInfo.stackedPlayerConnected.isEmpty) {
            context.become(receiver(gameActors - gameId))
          } else {
            val newChild = context.watch(context.actorOf(GameAntiChamber.props(gameId, layer)))
            gameInfo.stackedPlayerConnected.foreach(newChild ! _)
            context.become(receiver(gameActors + (gameId -> GameAntiChamberInfo(gameId, newChild))))
          }
      }

    case GameAntiChamberManagerFor(gameId) =>
      sender ! HereIsMaybeTheAntiChamberManagerFor(
        gameId,
        gameActors.get(gameId).filterNot(_.gameIsCancelling).map(_.ref)
      )
  }

  def receive: Receive = receiver(Map())

}

object JoinedGameDispatcher {

  case object SendHeartBeat
  case class NewClient(gameId: String, userId: String)
  case class GameAntiChamberManagerFor(gameId: String)
  case class HereIsMaybeTheAntiChamberManagerFor(gameId: String, ref: Option[ActorRef])

  private case class GameAntiChamberInfo(
      gameId: String,
      ref: ActorRef,
      stackedPlayerConnected: Set[GameAntiChamber.PlayerConnected] = Set(),
      isClosing: Boolean                                           = false,
      gameIsCancelling: Boolean                                    = false
  ) {
    def stackMessage(playerConnected: GameAntiChamber.PlayerConnected): GameAntiChamberInfo =
      copy(stackedPlayerConnected = stackedPlayerConnected + playerConnected)

    def closingMode: GameAntiChamberInfo = copy(isClosing = true)
    def openMode: GameAntiChamberInfo    = copy(isClosing = false)

    def cancelling: GameAntiChamberInfo = copy(gameIsCancelling = true)

    def clearStack: GameAntiChamberInfo = copy(stackedPlayerConnected = Set())
  }

  //def props: Props = Props(new JoinedGameDispatcher)

  final val name = "joined-game-dispatcher"

}
