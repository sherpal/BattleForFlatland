package websocketkeepers.gameantichamber

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import models.bff.gameantichamber.WebSocketProtocol
import models.users.User

import scala.util.Try

object AntiChamberClientTyped {

  sealed trait ProtocolSender
  final case object OutsideWorldSender extends ProtocolSender
  final case object GameAntiChamberSender extends ProtocolSender

  sealed trait Message
  final case class WebSocketProtocolWrapper(protocol: WebSocketProtocol, sender: ProtocolSender) extends Message

  sealed trait SetupMessage extends Message
  private final case object Broken extends SetupMessage

  /** Used by the GameAntiChamber client to tell who he is. */
  final case class HelloFromAntiChamber(ref: ActorRef[GameAntiChamberTyped.WebSocketProtocolWrapper])
      extends SetupMessage

  def apply(
      outerWorld: ActorRef[WebSocketProtocol],
      joinedGameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message],
      gameId: String,
      user: User
  ): Behavior[Message] =
    waitForAntiChamber(outerWorld, joinedGameDispatcherRef, gameId, user)

  private def receiver(
      outerWorld: ActorRef[WebSocketProtocol],
      antiChamberRef: ActorRef[GameAntiChamberTyped.WebSocketProtocolWrapper]
  ): Behavior[Message] =
    Behaviors
      .receive[Message] { (context, message) =>
        message match {
          case WebSocketProtocolWrapper(WebSocketProtocol.GameCancelled, _) =>
            outerWorld ! WebSocketProtocol.GameCancelled
            Behaviors.stopped
          case WebSocketProtocolWrapper(protocol, GameAntiChamberSender) =>
            outerWorld ! protocol
            Behaviors.same
          case WebSocketProtocolWrapper(protocol, OutsideWorldSender) =>
            antiChamberRef ! GameAntiChamberTyped.WebSocketProtocolWrapper(protocol, context.self)
            Behaviors.same
          case Broken                  => Behaviors.stopped
          case HelloFromAntiChamber(_) => Behaviors.unhandled
        }
      }
      .receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }

  private def waitForAntiChamber(
      outerWorld: ActorRef[WebSocketProtocol],
      joinedGameDispatcherRef: ActorRef[JoinedGameDispatcherTyped.Message],
      gameId: String,
      user: User
  ): Behavior[Message] = Behaviors.setup { context =>
    Try(context.watch(outerWorld)).getOrElse {
      context.self ! Broken
    }

    joinedGameDispatcherRef ! JoinedGameDispatcherTyped.NewClient(gameId, user, context.self)

    Behaviors
      .receiveMessagePartial[Message] {
        case Broken => Behaviors.stopped
        case HelloFromAntiChamber(ref) =>
          receiver(outerWorld, ref)
      }
      .receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
  }

}
