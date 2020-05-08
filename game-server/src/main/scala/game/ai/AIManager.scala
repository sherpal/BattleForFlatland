package game.ai

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
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

  def apply(actionTranslator: ActorRef[ActionTranslator.Message]): Behavior[Message] = receiver(
    ReceiverInfo(
      actionTranslator,
      GameState.initialGameState(0) // dummy game state when initialized
    )
  )

  /**
    * This class carries all the information the `receiver` needs.
    * Changing the receiver can thus be done much more easily.
    */
  private case class ReceiverInfo(
      actionTranslator: ActorRef[ActionTranslator.Message],
      lastGameState: GameState
  )

  private def receiver(
      receiverInfo: ReceiverInfo
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case HereIsTheGameState(gameState) =>
        receiver(receiverInfo.copy(lastGameState = gameState))
      case HereAreNewActions(newActions, idsToRemove) =>
        // we should first filter to keep only the actions we care about
        val unRemovedActions = newActions.filterNot(actions => idsToRemove.contains(actions.id))
        // todo
        Behaviors.same
    }
  }

}
