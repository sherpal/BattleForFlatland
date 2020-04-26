package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.gamestate.GameAction
import models.bff.gameantichamber.WebSocketProtocol
import models.bff.ingame.InGameWSProtocol

object ActionUpdateCollector {

  sealed trait Message
  case class NewExternalMember(actorRef: ActorRef[InGameWSProtocol]) extends Message
  case class NewInternalMember(actorRef: ActorRef[ExternalMessage]) extends Message

  sealed trait ExternalMessage extends Message {
    def toWebSocketProtocol: InGameWSProtocol
  }
  case class ActionsWereRemoved(oldestTime: Long, idsOfActionsToRemove: List[GameAction.Id]) extends ExternalMessage {
    override def toWebSocketProtocol: InGameWSProtocol =
      InGameWSProtocol.RemoveActions(oldestTime, idsOfActionsToRemove)
  }
  case class GameActionWrapper(gameActions: List[GameAction]) extends ExternalMessage {
    override def toWebSocketProtocol: InGameWSProtocol = InGameWSProtocol.GameActionWrapper(gameActions)
  }
  case class AddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ) extends ExternalMessage {
    override def toWebSocketProtocol: InGameWSProtocol = InGameWSProtocol.AddAndRemoveActions(
      actionsToAdd,
      oldestTimeToRemove,
      idsOfActionsToRemove
    )
  }

  /**
    * This actor is responsible for making the link between the messages the [[game.GameMaster]] will issue at the end
    * of each game loop.
    *
    * External members are actors that are linked to their actual "masters" via web socket. Mostly the players.
    * Internal members are actors that are inside this JVM/JS and can be contacted directly.
    */
  def apply(
      externalMembers: List[ActorRef[InGameWSProtocol]],
      internalMembers: List[ActorRef[ExternalMessage]]
  ): Behavior[Message] =
    Behaviors.receiveMessage {
      case NewExternalMember(actorRef) => apply(actorRef +: externalMembers, internalMembers)
      case NewInternalMember(actorRef) => apply(externalMembers, actorRef +: internalMembers)
      case message: ExternalMessage =>
        val protocolMessage = message.toWebSocketProtocol
        externalMembers.foreach(_ ! protocolMessage)
        internalMembers.foreach(_ ! message)
        Behaviors.same
    }

}
