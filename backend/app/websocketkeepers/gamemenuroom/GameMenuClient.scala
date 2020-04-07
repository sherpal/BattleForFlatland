package websocketkeepers.gamemenuroom

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

import scala.util.Try

/**
  * Actor representing the the web socket client in the game menu room.
  * @param outerWorld actor given by the `ActorFlow.actorRef` method to send message back to the client.
  */
final class GameMenuClient(outerWorld: ActorRef, gameMenuRoomBookKeeper: ActorRef) extends Actor {

  override def preStart(): Unit = {
    Try(context.watch(outerWorld)).getOrElse {
      self ! PoisonPill // we we can't even watch this actor, let's just die.
    }

    gameMenuRoomBookKeeper ! GameMenuRoomBookKeeper.NewClient
  }

  override def receive: Receive = {
    case GameMenuRoomBookKeeper.NewGame =>
      outerWorld ! """ "new-game" """
    case GameMenuRoomBookKeeper.SendHeartBeat =>
      outerWorld ! """ "" """
    case Terminated(_) =>
      self ! PoisonPill
  }

}

object GameMenuClient {

  def props(outerWorld: ActorRef, gameMenuRoomBookKeeper: ActorRef): Props = Props {
    new GameMenuClient(outerWorld, gameMenuRoomBookKeeper)
  }

}
