package game.ai

import akka.actor.typed.ActorRef
import gamelogic.gamestate.{GameAction, GameState}
import zio.ZIO
import zio.clock.Clock

/**
  * Messages sent to any AI controller.
  *
  * All these actors need to now the last [[gamelogic.gamestate.GameState]], and (potentially) all actions that
  * occur (not sure if this is necessary, though...).
  *
  * This, in particular, means that nothing in the type system will remember what type of the actor ref the
  * [[game.ai.AIManager]] will know, which I guess is not an issue. It only need to create the right actor when
  * receiving message, and that is insured by the signature of the actor constructors.
  */
sealed trait AIControllerMessage

object AIControllerMessage {

  /**
    * This is sent by the [[game.ai.AIManager]] to tell controllers that the state has changed
    */
  case class GameStateWrapper(gameState: GameState) extends AIControllerMessage

  /** Sent by the [[game.ai.AIManager]] to tell controllers that new actions have been taken. */
  case class NewActions(actions: List[GameAction]) extends AIControllerMessage

  /** Sent by a controller to itself to tell itself to take decisions. */
  case object Loop extends AIControllerMessage

  private def sendMeLoop(to: ActorRef[Loop.type], in: zio.duration.Duration) =
    for {
      fiber <- zio.clock.sleep(in).fork
      _ <- fiber.join
      _ <- ZIO.effectTotal(to ! Loop)
    } yield ()

  def unsafeRunSendMeLoop(to: ActorRef[Loop.type], in: zio.duration.Duration): Unit =
    zio.Runtime.default.unsafeRunAsync(sendMeLoop(to, in))(_ => ())

}
