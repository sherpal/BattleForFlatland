package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import models.bff.gameantichamber.WebSocketProtocol.PlayerJoined

final class GameAntiChamber(gameId: String) extends Actor with ActorLogging {

  import GameAntiChamber._

  /**
    * Remembers the list of web socket clients (the player) that are connected to this game.
    */
  def receiver(players: Map[ActorRef, String]): Actor.Receive = {
    val clients = players.keys

    {
      case JoinedGameDispatcher.SendHeartBeat =>
        clients.foreach(_ ! JoinedGameDispatcher.SendHeartBeat)
      case PlayerConnected(ref, userId) =>
        context.watch(ref)
        context.become(receiver(players + (ref -> userId)))
        clients.foreach(_ ! PlayerJoined)
      case Terminated(ref) =>
        val newPlayersSet = players - ref
        context.become(receiver(newPlayersSet))
        if (newPlayersSet.isEmpty) context.parent ! IAmEmpty
      case YouCanClose =>
        if (players.isEmpty) {
          self ! PoisonPill
        } else {
          log.debug("Received a YouCanClose order but I'm not empty, crazy.")
          context.parent ! DidNotClose
        }
    }
  }

  def receive: Actor.Receive = receiver(Map())

}

object GameAntiChamber {

  case class PlayerConnected(ref: ActorRef, userId: String)
  case object IAmEmpty
  case object YouCanClose
  case object DidNotClose

  def props(gameId: String): Props = Props(new GameAntiChamber(gameId))

}
