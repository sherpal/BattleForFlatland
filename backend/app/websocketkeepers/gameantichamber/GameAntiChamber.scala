package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.gameantichamber.WebSocketProtocol.GameStatusUpdated

final class GameAntiChamber(gameId: String) extends Actor with ActorLogging {

  import GameAntiChamber._

  /**
    * Remembers the list of web socket clients (the player) that are connected to this game.
    */
  def receiver(players: Map[ActorRef, String], cancelling: Boolean = false): Actor.Receive = {
    val clients = players.keys

    {
      case JoinedGameDispatcher.SendHeartBeat =>
        // keeping connection alive
        clients.foreach(_ ! WebSocketProtocol.HeartBeat)
      case PlayerConnected(ref, userId) =>
        // a new player has connected. We tell it who we are, we notify all the others that a new client arrived
        // and we add it to the list
        context.watch(ref) ! Hello
        context.become(receiver(players + (ref -> userId)))
        clients.foreach(_ ! GameStatusUpdated)
      case Terminated(ref) =>
        // a client is dead, perhaps because the client refreshed their pages or actually left
        // either way, we remove it from the set of players, and we notify our parent if we are out of players
        val newPlayersSet = players - ref
        context.become(receiver(newPlayersSet))
        if (newPlayersSet.isEmpty) context.parent ! IAmEmpty
      case YouCanClose =>
        // parent has done what needed to be done for me to close properly
        // if a PlayerConnected message happened to arrive in between (very unlikely!), we notify the parent that we
        // actually can't close
        if (players.isEmpty) {
          println("no more connected players")
          self ! PoisonPill
        } else {
          log.debug("Received a YouCanClose order but I'm not empty, crazy.")
          context.parent ! DidNotClose
        }
      case YouCanCleanUpCancel =>
        // parent is notified that the game is cancelled, and gave us the go to do so
        clients.foreach(_ ! WebSocketProtocol.GameCancelled)
        clients.foreach(context.unwatch)
        self ! PoisonPill
      case CancelGame =>
        // game creator has cancelled it (either willingly, or not)
        // we notify the parent that we need to cancel
        context.parent ! CancelGame

    }
  }

  def receive: Actor.Receive = receiver(Map())

}

object GameAntiChamber {

  case class PlayerConnected(ref: ActorRef, userId: String)
  case object IAmEmpty
  case object YouCanClose
  case object YouCanCleanUpCancel
  case object DidNotClose
  case object Hello // notifying an AntiChamberClient that I take care of the game
  case object CancelGame

  def props(gameId: String): Props = Props(new GameAntiChamber(gameId))

}
