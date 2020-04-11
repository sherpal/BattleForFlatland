package websocketkeepers.gameantichamber

import akka.actor.{Actor, ActorRef, Props, Terminated}
import javax.inject.Singleton

import scala.concurrent.duration._

/**
  * The role of the [[JoinedGameDispatcher]] is to listen to WebSocket connections
  */
@Singleton
final class JoinedGameDispatcher extends Actor {

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
  def receiver(gameActors: Map[String, (ActorRef, Set[GameAntiChamber.PlayerConnected], Boolean)]): Actor.Receive = {
    case SendHeartBeat =>
      gameActors.values.map(_._1).foreach(_ ! SendHeartBeat) // keeping connections alive
    case NewClient(gameId, userId) => // a new web socket client arrives
      gameActors.get(gameId) match {
        case Some(ref) if !ref._3 => // this game is already taken care of, and not waiting, we notify the actor
          ref._1 ! GameAntiChamber.PlayerConnected(sender, userId)
        case Some(ref) if ref._3 => // this game already exists, but waiting, we stack the message
          context.become(
            receiver(gameActors + (gameId -> ref.copy(_2 = ref._2 + GameAntiChamber.PlayerConnected(sender, userId))))
          )
        case None => // this game does not exist, we create it and start over
          context.become(
            receiver(
              gameActors + (gameId -> (context.watch(context.actorOf(GameAntiChamber.props(gameId))), Set(), false))
            )
          )
          self forward NewClient(gameId, userId)
      }
    case GameAntiChamber.IAmEmpty => // a child has no more connection. We tell it to close and put it in waiting mode
      sender ! GameAntiChamber.YouCanClose
      gameActors.find(_._2._1 == sender).foreach {
        case (gameId, (ref, stackedMessage, _)) =>
          context.become(
            receiver(gameActors + (gameId -> (ref, stackedMessage, true)))
          )
      }
    case GameAntiChamber.DidNotClose =>
      // the child did not close because by the time it received the YouCanClose message,
      // it actually received other PlayerConnected Message.
      // We send it all the message that we stacked until then and clear the stack, putting its status
      // as non waiting again.
      gameActors.find(_._2._1 == sender).foreach {
        case (gameId, (_, stackedMessage, _)) =>
          stackedMessage.foreach(sender ! _)
          context.become(
            receiver(
              gameActors + (gameId -> (sender, Set(), false))
            )
          )
      }
    case Terminated(child) =>
      // The child actually was killed. If we still have stacked messages, we create a new one and send it all the
      // stacked messages.
      gameActors.find(_._2._1 == child).foreach {
        case (gameId, (_, stackedMessage, _)) =>
          if (stackedMessage.isEmpty) {
            context.become(receiver(gameActors - gameId))
          } else {
            val newChild = context.watch(context.actorOf(GameAntiChamber.props(gameId)))
            stackedMessage.foreach(newChild ! _)
            context.become(receiver(gameActors + (gameId -> (newChild, Set(), false))))
          }
      }

  }

  def receive: Receive = receiver(Map())

}

object JoinedGameDispatcher {

  case object SendHeartBeat
  case class NewClient(gameId: String, userId: String)

  def props: Props = Props(new JoinedGameDispatcher)

  final val name = "game-menu-room-book-keeper"

}
