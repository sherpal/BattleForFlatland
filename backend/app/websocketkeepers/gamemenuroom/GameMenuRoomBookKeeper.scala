package websocketkeepers.gamemenuroom

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import javax.inject.Singleton
import websocketkeepers.gamemenuroom.GameMenuRoomBookKeeper.{NewClient, NewGame, SendHeartBeat}

import scala.concurrent.duration._

@Singleton
final class GameMenuRoomBookKeeper extends Actor with ActorLogging {

  import context.dispatcher

  override def preStart(): Unit =
    context.system.scheduler.scheduleAtFixedRate(
      5.seconds,
      5.seconds,
      self,
      SendHeartBeat
    )

  def receiver(currentClients: Set[ActorRef]): Actor.Receive = {
    case SendHeartBeat =>
      currentClients.foreach(_ ! SendHeartBeat) // maintaining connection alive
    case NewClient =>
      println("new web socket client")
      sender ! NewGame
      context.watch(sender)
      context.become(receiver(currentClients + sender))
    case NewGame =>
      currentClients.foreach(_ ! NewGame)
    case Terminated(ref) =>
      println("web socket client terminated")
      context.become(receiver(currentClients - ref))
  }

  def receive: Receive = receiver(Set())

}

object GameMenuRoomBookKeeper {

  final case class NewClient(actorRef: ActorRef)
  final case object NewGame
  final case object SendHeartBeat

  def props: Props = Props(new GameMenuRoomBookKeeper)

  final val name = "game-menu-room-book-keeper"

}
