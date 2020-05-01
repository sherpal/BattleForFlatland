package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.gamestate.GameAction
import models.bff.ingame.InGameWSProtocol

object ActionUpdateCollector {

  sealed trait Message

  /** A new external actor will need to be notified by the new messages.
    * These will mostly arrive at the beginning of the game.
    */
  case class NewExternalMember(actorRef: ActorRef[InGameWSProtocol.Incoming]) extends Message

  /** A new internal actor (IA) will need to be notified by the new messages.
    * These will arrive during the game, when new AI are created.
    */
  case class NewInternalMember(actorRef: ActorRef[ExternalMessage]) extends Message

  /**
    * An existing internal member doesn't need to be notified anymore.
    * This may arrive when an IA is removed from the game.
    */
  case class RemoveInternalMember(actorRef: ActorRef[ExternalMessage]) extends Message

  sealed trait ExternalMessage extends Message {
    def toWebSocketProtocol: InGameWSProtocol.Incoming
  }
  case class ActionsWereRemoved(oldestTime: Long, idsOfActionsToRemove: List[GameAction.Id]) extends ExternalMessage {
    override def toWebSocketProtocol: InGameWSProtocol.Incoming =
      InGameWSProtocol.RemoveActions(oldestTime, idsOfActionsToRemove)
  }
  case class AddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ) extends ExternalMessage {
    override def toWebSocketProtocol: InGameWSProtocol.Incoming = InGameWSProtocol.AddAndRemoveActions(
      actionsToAdd,
      oldestTimeToRemove,
      idsOfActionsToRemove
    )
  }

  /**
    * This actor is responsible for making the link between the messages the [[game.GameMaster]] will issue at the end
    * of each game loop.
    */
  def apply(): Behavior[Message] = receiver(Nil, Nil)

  /**
    * External members are actors that are linked to their actual "masters" via web socket. Mostly the players.
    * Internal members are actors that are inside this JVM/JS and can be contacted directly.
    */
  private def receiver(
      externalMembers: List[ActorRef[InGameWSProtocol.Incoming]],
      internalMembers: List[ActorRef[ExternalMessage]]
  ): Behavior[Message] =
    Behaviors.receiveMessage {
      case NewExternalMember(actorRef)    => receiver(actorRef +: externalMembers, internalMembers)
      case NewInternalMember(actorRef)    => receiver(externalMembers, actorRef +: internalMembers)
      case RemoveInternalMember(actorRef) => receiver(externalMembers, internalMembers.filterNot(_ == actorRef))
      case message: ExternalMessage =>
        val protocolMessage = message.toWebSocketProtocol
        externalMembers.foreach(_ ! protocolMessage)
        internalMembers.foreach(_ ! message)
        Behaviors.same
    }

}
