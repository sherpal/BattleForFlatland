package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import akka.stream.typed.scaladsl.ActorSink
import gamelogic.gamestate.GameAction
import models.bff.ingame.InGameWSProtocol

import scala.concurrent.duration._

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

  def apply(gameMaster: ActorRef[GameMaster.Message]): Behavior[Message] = Behaviors.setup { context =>
    implicit val system: ActorSystem[_] = context.system
    receiver(gameMaster, makeQueue(gameMaster))
  }

  /**
    * Messages from AI will come in a very high through put, and it's useless that the [[game.GameMaster]] receives
    * one message for every one of them.
    *
    * Messages are thus grouped within 33 milliseconds and then sent to the game master.
    */
  private def makeQueue(
      to: ActorRef[GameMaster.Message]
  )(implicit materializer: Materializer): SourceQueueWithComplete[List[GameAction]] =
    Source
      .queue[List[GameAction]](10, OverflowStrategy.dropNew)
      .groupedWithin(20, 33.millis)
      .map(_.flatten.toList)
      .map(GameMaster.MultipleActionsWrapper)
      .to(ActorSink.actorRef(to, GameMaster.MultipleActionsWrapper(Nil), _ => GameMaster.MultipleActionsWrapper(Nil)))
      .run()

  private def receiver(
      gameMaster: ActorRef[GameMaster.Message],
      queue: SourceQueueWithComplete[List[GameAction]]
  ): Behavior[Message] =
    Behaviors.receiveMessage {
      case InGameWSProtocolWrapper(actionsWrapper) =>
        // todo: handle integrity (or maybe not? it's a coop game, after all)
        gameMaster ! GameMaster.MultipleActionsWrapper(actionsWrapper.gameActions)
        Behaviors.same
      case GameActionsWrapper(gameActions) =>
        queue.offer(gameActions)
        Behaviors.same
    }

}
