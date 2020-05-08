package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ai.AIManager
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.ingame.InGameWSProtocol

object ActionUpdateCollector {

  sealed trait Message

  /** A new external actor will need to be notified by the new messages.
    * These will mostly arrive at the beginning of the game.
    */
  case class NewExternalMember(actorRef: ActorRef[InGameWSProtocol.Incoming]) extends Message

  /**
    * Tells the [[game.ActionUpdateCollector]] who is the [[game.ai.AIManager]], so that it can send it
    * messages as well.
    */
  case class HereIsTheAIManager(actorRef: ActorRef[AIManager.Message]) extends Message

  sealed trait ExternalMessage extends Message

  case class AddAndRemoveActions(
      actionsToAdd: List[GameAction],
      oldestTimeToRemove: Long,
      idsOfActionsToRemove: List[GameAction.Id]
  ) extends ExternalMessage {
    def toWebSocketProtocol: InGameWSProtocol.Incoming = InGameWSProtocol.AddAndRemoveActions(
      actionsToAdd,
      oldestTimeToRemove,
      idsOfActionsToRemove
    )
  }

  case class GameStateWrapper(gameState: GameState) extends ExternalMessage

  /**
    * This actor is responsible for making the link between the messages the [[game.GameMaster]] will issue at the end
    * of each game loop.
    */
  def apply(): Behavior[Message] = receiver(None, Nil)

  /**
    * External members are actors that are linked to their actual "masters" via web socket. Mostly the players.
    * Internal members are actors that are inside this JVM/JS and can be contacted directly.
    */
  private def receiver(
      aiManager: Option[ActorRef[AIManager.Message]],
      externalMembers: List[ActorRef[InGameWSProtocol.Incoming]]
  ): Behavior[Message] =
    Behaviors.receiveMessage {
      case message: AddAndRemoveActions =>
        val protocolMessage = message.toWebSocketProtocol
        externalMembers.foreach(_ ! protocolMessage)
        aiManager.foreach(_ ! AIManager.HereAreNewActions(message.actionsToAdd, message.idsOfActionsToRemove))
        Behaviors.same
      case GameStateWrapper(gameState) =>
        aiManager.foreach(_ ! AIManager.HereIsTheGameState(gameState))
        Behaviors.same
      case NewExternalMember(actorRef)  => receiver(aiManager, actorRef +: externalMembers)
      case HereIsTheAIManager(actorRef) => receiver(Some(actorRef), externalMembers)
    }

}
