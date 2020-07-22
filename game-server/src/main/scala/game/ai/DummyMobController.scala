package game.ai

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIManager.loopRate
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{AddDummyMob, MovingBodyMoves}

import scala.Ordering.Double.TotalOrdering
import scala.concurrent.duration._

object DummyMobController {

  @inline private def now = System.currentTimeMillis

  def apply(
      actionTranslator: ActorRef[ActionTranslator.Message],
      addDummyMob: AddDummyMob
  ): Behavior[AIControllerMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        AIControllerMessage.unsafeRunSendMeLoop(context.self, zio.duration.Duration.fromScala(loopRate.millis))
        receiver(actionTranslator, addDummyMob, gameState)
      case _ =>
        //waiting for first game state
        Behaviors.same
    }
  }

  private def receiver(
      actionTranslator: ActorRef[ActionTranslator.Message],
      addDummyMob: AddDummyMob,
      currentGameState: GameState
  ): Behavior[AIControllerMessage] = Behaviors.receive { (context, message) =>
    def myId = addDummyMob.entityId

    message match {
      case AIControllerMessage.Loop =>
        // Here the dummy mob should exist, otherwise the behaviour would have been stopped, due to how the game state
        // receiver is implemented.
        // In any case, if it doesn't, the exception will be thrown and the actor will be stopped anyway.
        val me              = currentGameState.dummyMobs(myId)
        val startTime       = now
        val currentPosition = me.currentPosition(startTime)

        val rotation = currentGameState.players.values
          .map(_.currentPosition(startTime))
          .minByOption(
            pos => (pos - currentPosition).modulus
          )
          .map(pos => (pos - currentPosition).arg)
          .getOrElse(0.0)

        if (math.abs(rotation - me.rotation) > 1e-6) {
          actionTranslator ! ActionTranslator.GameActionsWrapper(
            MovingBodyMoves(0L, startTime, myId, currentPosition, rotation, rotation, me.speed, moving = true) :: Nil
          )
        }

        val timeTaken = now - startTime
        AIControllerMessage.unsafeRunSendMeLoop(
          context.self,
          zio.duration.Duration.fromScala(
            ((loopRate - timeTaken) max 0).millis
          )
        )

        Behaviors.same
      case AIControllerMessage.GameStateWrapper(gameState) =>
        gameState.dummyMobs
          .get(myId)
          .fold(Behaviors.stopped[AIControllerMessage])(_ => receiver(actionTranslator, addDummyMob, gameState))
      case AIControllerMessage.NewActions(_) =>
        Behaviors.same // we do nothing here (currently)
      case AIControllerMessage.ObstacleGraph(_) =>
        Behaviors.unhandled
    }
  }

}
