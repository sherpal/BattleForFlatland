package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.gameantichamber.WebSocketProtocol.HeartBeat
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
      self ! PoisonPill // we can't even watch this actor, let's just die.
    }

    joinedGameDispatcher ! JoinedGameDispatcher.NewClient(gameId, user.userId)
  }

  def receive: Actor.Receive = {
    case JoinedGameDispatcher.SendHeartBeat =>
      outerWorld ! HeartBeat
    case m: WebSocketProtocol =>
      outerWorld ! m
    case Terminated(child) =>
      if (child == outerWorld) {
        self ! PoisonPill
      }
  }

}

object AntiChamberClient {

  def props(outerWorld: ActorRef, joinedGameDispatcher: ActorRef, gameId: String, user: User): Props = Props(
    new AntiChamberClient(outerWorld, joinedGameDispatcher, gameId, user)
  )

}
