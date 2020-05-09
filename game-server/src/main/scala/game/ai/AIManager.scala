package game.ai

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import gamelogic.gamestate.gameactions.AddDummyMob
import gamelogic.gamestate.{GameAction, GameState}

/**
  * The [[game.ai.AIManager]] is responsible for spawning and removing "artificial intelligence" actors that will
  * control game entities, outside of players.
  *
  * The idea is that each [[gamelogic.entities.Entity]] (that must take actions, so not a
  * [[gamelogic.entities.SimpleBulletBody]], for example) will be handled by a different actor. If some entities have
  * a very similar behaviour, or if they need to be coordinated, they could handled together by a single actor.
  *
  * AI actors will typically run at around 30 fps, which is way more than enough to have a meaningful behaviour.
  *
  * The AI manager will receive the [[gamelogic.gamestate.GameState]] computed by the [[game.GameMaster]], and will
  * dispatch the information to every AI actor there is.
  */
object AIManager {

  sealed trait Message

  /**
    * Refreshes the current game state that the AI manager knows about.
    * It will be in charge of telling all the AI about it.
    * We keep a reference to it so that newly created ai actors can have it immediately.
    *
    * Note: it is the responsibility of the AIManager to be sure that each AI actor has the proper game state
    * information.
    */
  case class HereIsTheGameState(gameState: GameState) extends Message

  /**
    * These are the new actions that the server treated in its last iteration.
    * We also receive the ids of the actions that have been removed, so that we can immediately remove from the
    * `newAction` list the actions that have been removed.
    *
    * The AIManager will treat this message by spawning or deleting any actor that was affected by these actions.
    */
  case class HereAreNewActions(newActions: List[GameAction], idsToRemove: List[Long]) extends Message

  private case class ControllerDied(ref: ActorRef[Nothing]) extends Message

  def apply(actionTranslator: ActorRef[ActionTranslator.Message]): Behavior[Message] = receiver(
    ReceiverInfo(
      actionTranslator,
      GameState.initialGameState(0), // dummy game state when initialized
      Set.empty
    )
  )

  /**
    * This class carries all the information the `receiver` needs.
    * Changing the receiver can thus be done much more easily.
    *
    * This class has facility methods for updating its content, all of which using the underlying `copy` method under
    * the hood. This allows to add more semantic in the receiver implementation.
    */
  private case class ReceiverInfo(
      actionTranslator: ActorRef[ActionTranslator.Message],
      lastGameState: GameState,
      entityControllers: Set[ActorRef[AIControllerMessage]]
  ) {

    def withGameState(gameState: GameState): ReceiverInfo = copy(lastGameState = gameState)

    def addEntityControllers(newEntityControllers: Iterable[ActorRef[AIControllerMessage]]): ReceiverInfo =
      copy(entityControllers = newEntityControllers.toSet ++ entityControllers)

    def removeEntityController(ref: ActorRef[Nothing]): ReceiverInfo =
      copy(entityControllers = entityControllers - ref.unsafeUpcast[AIControllerMessage])

  }

  private def receiver(
      receiverInfo: ReceiverInfo
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    def broadcastMessage(message: AIControllerMessage): Unit =
      receiverInfo.entityControllers.foreach(_ ! message)

    message match {
      case HereIsTheGameState(gameState) =>
        broadcastMessage(AIControllerMessage.GameStateWrapper(gameState))

        receiver(receiverInfo.withGameState(gameState))
      case HereAreNewActions(newActions, idsToRemove) =>
        val unRemovedActions = newActions
          .filterNot(actions => idsToRemove.contains(actions.id))

        val newEntityControllers = unRemovedActions.collect {
          case action: AddDummyMob =>
            val ref =
              context.spawn(
                DummyMobController(receiverInfo.actionTranslator, action),
                s"DummyMob-${action.entityId}"
              )

            context.watchWith(ref, ControllerDied(ref))

            ref
        }.toSet

        newEntityControllers.foreach(_ ! AIControllerMessage.GameStateWrapper(receiverInfo.lastGameState))
        if (newActions.nonEmpty) {
          broadcastMessage(AIControllerMessage.NewActions(unRemovedActions))
        }

        if (newEntityControllers.isEmpty) Behaviors.same
        else
          receiver(receiverInfo.addEntityControllers(newEntityControllers))

      case ControllerDied(ref) =>
        receiver(receiverInfo.removeEntityController(ref))
    }
  }

}
