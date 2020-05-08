package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.gamestate.GameAction
import models.bff.ingame.InGameWSProtocol

/**
  * This actor is responsible for receiving messages from game entities, and pass them to the game master.
  *
  * He is responsible for
  * - forwarding messages to the [[game.GameMaster]], translating them correctly if need be
  * - be sure that messages coming from outside belong to the right person (you can't move someone else's entity)
  * //todo
  */
object ActionTranslator {

  sealed trait Message

  /** This is a message from outside. We will need to check its integrity. */
  case class InGameWSProtocolWrapper(actionsWrapper: InGameWSProtocol.GameActionWrapper) extends Message

  /** Message from this JVM, we can trust it. */
  case class GameActionsWrapper(gameActions: List[GameAction]) extends Message

  def apply(gameMaster: ActorRef[GameMaster.Message]): Behavior[Message] = Behaviors.receiveMessage {
    case InGameWSProtocolWrapper(actionsWrapper) =>
      // todo: handle integrity (or maybe not? it's a coop game, after all)
      gameMaster ! GameMaster.MultipleActionsWrapper(actionsWrapper.gameActions)
      Behaviors.same
    case GameActionsWrapper(gameActions) =>
      gameMaster ! GameMaster.MultipleActionsWrapper(gameActions)
      Behaviors.same
  }

}
