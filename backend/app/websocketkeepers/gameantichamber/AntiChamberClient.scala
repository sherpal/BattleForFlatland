package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorRef, Props, Terminated}
import models.bff.gameantichamber.WebSocketProtocol
import models.users.User

import scala.util.Try

/**
  * Actor representing the web socket client when a game was joined.
  *
  * @param outerWorld actor given by the `ActorFlow.actorRef` method to send message back to the client.
  */
final class AntiChamberClient(outerWorld: ActorRef, joinedGameDispatcher: ActorRef, gameId: String, user: User)
    extends Actor {

  override def preStart(): Unit = {
    Try(context.watch(outerWorld)).getOrElse {
      context.stop(self) // we can't even watch this actor, let's just die.
    }

    joinedGameDispatcher ! JoinedGameDispatcher.NewClient(gameId, user.userId)
  }

  def receiver(antiChamberActor: ActorRef): Actor.Receive = {
    case WebSocketProtocol.GameCancelled =>
      // if the message is to cancel the game, we close the connection
      outerWorld ! WebSocketProtocol.GameCancelled
      context.stop(self)
    case m: WebSocketProtocol if sender == antiChamberActor =>
      // anti chamber manager has sent order for the outside world, we forward it
      outerWorld ! m
    case m: WebSocketProtocol =>
      // outside world has sent a message, we send it to the anti chamber manager on our behalf, so that it can check
      // if I have permission to do that
      antiChamberActor ! m
    case Terminated(child) =>
      if (child == outerWorld) {
        // web socket connection is closed, we kill ourselves
        context.stop(self)
      }

    case m =>
      println(s"hey, $m")
  }

  def receive: Actor.Receive = {
    case GameAntiChamber.Hello =>
      context.become(receiver(sender))
    case Terminated(child) =>
      if (child == outerWorld) {
        context.stop(self)
      }
    case m =>
      println("weird", m)
  }

}

object AntiChamberClient {

  def props(outerWorld: ActorRef, joinedGameDispatcher: ActorRef, gameId: String, user: User): Props = Props(
    new AntiChamberClient(outerWorld, joinedGameDispatcher, gameId, user)
  )

}
