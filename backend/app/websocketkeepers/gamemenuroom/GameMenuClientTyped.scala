package websocketkeepers.gamemenuroom

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.Try

object GameMenuClientTyped {

  sealed trait Message
  case object GameListUpdate extends Message
  case object SendHeartBeat extends Message
  private case object Broken extends Message
  case object Dummy extends Message // ignoring messages from the frontend

  def apply(
      outerWorld: ActorRef[String],
      gameMenuRoomBookKeeper: ActorRef[GameMenuRoomBookKeeperTyped.Message]
  ): Behavior[Message] =
    Behaviors.setup { context =>
      Try(context.watchWith(outerWorld, Broken)).getOrElse {
        context.self ! Broken
      }

      gameMenuRoomBookKeeper ! GameMenuRoomBookKeeperTyped.NewClient(context.self)

      Behaviors.receiveMessage {
        case GameListUpdate =>
          outerWorld ! """ "new-game" """
          Behaviors.same
        case SendHeartBeat =>
          outerWorld ! """ "" """
          Behaviors.same
        case Broken =>
          Behaviors.stopped
        case Dummy => Behaviors.unhandled
      }

    }

}
