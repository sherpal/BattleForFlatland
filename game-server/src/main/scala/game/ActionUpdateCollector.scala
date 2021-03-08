package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ai.AIManager
import gamelogic.gamestate.{GameAction, GameState}
import models.bff.ingame.InGameWSProtocol
import game.ai.goodais.GoodAIManager
import gamelogic.entities.Entity
import models.bff.outofgame.gameconfig.PlayerName

import scala.concurrent.duration._

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

  /**
    * Tells the [[game.ActionUpdateCollector]] who is the [[game.ai.goodais.GoodAIManager]], so that it can send it
    * messages as well.
    */
  case class HereIsTheGoodAIManager(actorRef: ActorRef[GoodAIManager.Command]) extends Message

  /**
    * Tells the [[game.ActionUpdateCollector]] who is the [[game.ai.AIManager]], so that it can send it
    * messages as well.
    */
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
    * Sent by the GameMaster at the beginning of the game to tell the GoodAIManager what are the
    * entity ids of the AI players
    *
    * @param idsAndNames list of pairs containing the entity id with the name of the entity.
    */
  case class EntityIdsAndNamesForAIs(idsAndNames: List[(Entity.Id, PlayerName.AIPlayerName)]) extends ExternalMessage

  /**
    * This actor is responsible for making the link between the messages the [[game.GameMaster]] will issue at the end
    * of each game loop.
    */
  def apply(): Behavior[Message] = receiver(Option.empty, Option.empty, Nil)

  /**
    * External members are actors that are linked to their actual "masters" via web socket. Mostly the players.
    * Internal members are actors that are inside this JVM/JS and can be contacted directly.
    */
  private def receiver(
      aiManager: Option[ActorRef[AIManager.Message]],
      goodAIManager: Option[ActorRef[GoodAIManager.Command]],
      externalMembers: List[ActorRef[InGameWSProtocol.Incoming]]
  ): Behavior[Message] =
    Behaviors.receive { (context, command) =>
      command match {
        case message: AddAndRemoveActions =>
          val protocolMessage = message.toWebSocketProtocol
          externalMembers.foreach(_ ! protocolMessage)
          aiManager.foreach(_ ! AIManager.HereAreNewActions(message.actionsToAdd, message.idsOfActionsToRemove))
          goodAIManager.foreach(_ ! GoodAIManager.HereAreNewActions(message.actionsToAdd, message.idsOfActionsToRemove))
          Behaviors.same
        case GameStateWrapper(gameState) =>
          aiManager.foreach(_ ! AIManager.HereIsTheGameState(gameState))
          goodAIManager.foreach(_ ! GoodAIManager.HereIsTheGameState(gameState))
          Behaviors.same
        case NewExternalMember(actorRef)      => receiver(aiManager, goodAIManager, actorRef +: externalMembers)
        case HereIsTheAIManager(actorRef)     => receiver(Some(actorRef), goodAIManager, externalMembers)
        case HereIsTheGoodAIManager(actorRef) => receiver(aiManager, Some(actorRef), externalMembers)
        case cmd @ EntityIdsAndNamesForAIs(elements) =>
          goodAIManager match {
            case None =>
              context.log.warn("Received entity ids for AIs before the manager, retrying in 1s")
              context.scheduleOnce(1.second, context.self, cmd)
            case Some(value) =>
              value ! GoodAIManager.EntityIdsWithNames(elements)
          }
          Behaviors.same
      }
    }

}
