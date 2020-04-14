package websocketkeepers.gamemenuroom

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import play.api.libs.concurrent.ActorModule

import scala.concurrent.duration._

object GameMenuRoomBookKeeperTyped extends ActorModule {

  sealed trait Message
  final case class NewClient(actorRef: ActorRef[GameMenuClientTyped.Message]) extends Message
  final case object GameListUpdate extends Message
  final case object SendHeartBeat extends Message
  private case class ClientDead(actorRef: ActorRef[GameMenuClientTyped.Message]) extends Message

  @Provides
  def apply(): Behavior[Message] = Behaviors.withTimers { timerScheduler =>
    timerScheduler.startTimerAtFixedRate(SendHeartBeat, 5.seconds)
    behavior(Set())
  }

  private def behavior(currentClients: Set[ActorRef[GameMenuClientTyped.Message]]): Behavior[Message] =
    Behaviors
      .receive[Message] { (context, message: Message) =>
        message match {
          case NewClient(actorRef) =>
            actorRef ! GameMenuClientTyped.GameListUpdate
            context.watchWith(actorRef, ClientDead(actorRef))
            behavior(currentClients + actorRef)
          case GameListUpdate =>
            currentClients.foreach(_ ! GameMenuClientTyped.GameListUpdate)
            Behaviors.same
          case SendHeartBeat =>
            currentClients.foreach(_ ! GameMenuClientTyped.SendHeartBeat)
            Behaviors.same
          case ClientDead(ref) =>
            behavior(currentClients - ref)
        }
      }

}
