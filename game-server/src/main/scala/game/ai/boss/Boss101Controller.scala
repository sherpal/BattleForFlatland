package game.ai.boss

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIControllerMessage
import game.ai.AIManager.loopRate
import gamelogic.entities.boss.Boss101
import gamelogic.gamestate.GameState
import gamelogic.gamestate.gameactions.{MovingBodyMoves, SpawnBoss}

import scala.concurrent.duration._

object Boss101Controller {

  @inline private def now = System.currentTimeMillis

  def apply(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialMessage: SpawnBoss
  ): Behavior[AIControllerMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        AIControllerMessage.unsafeRunSendMeLoop(context.self, zio.duration.Duration.fromScala(loopRate.millis))
        receiver(actionTranslator, spawnBoss = initialMessage, currentGameState = gameState)
      case _ =>
        //waiting for first game state
        Behaviors.same
    }
  }

  private def receiver(
      actionTranslator: ActorRef[ActionTranslator.Message],
      spawnBoss: SpawnBoss,
      currentGameState: GameState
  ): Behavior[AIControllerMessage] = Behaviors.receive { (context, message) =>
    def myId = spawnBoss.entityId

    message match {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        gameState.bosses
          .get(myId)
          .fold(Behaviors.stopped[AIControllerMessage])(
            _ =>
              receiver(
                actionTranslator,
                spawnBoss,
                gameState
              )
          )
      case AIControllerMessage.NewActions(_) => Behaviors.same
      case AIControllerMessage.Loop =>
        val startTime       = now
        val me              = currentGameState.bosses(myId)
        val currentPosition = me.currentPosition(startTime)

        val maybeMoveAction = me.damageThreats
          .maxByOption(_._2)
          .map(_._1)
          .flatMap(
            currentGameState.players.get // this could change in the future
          )
          .fold(currentGameState.players.headOption.map(_._2))(Some(_))
          .map { target =>
            val targetPosition = target.currentPosition(startTime)

            val directionVector = targetPosition - currentPosition

            MovingBodyMoves(
              0L,
              startTime,
              myId,
              currentPosition,
              directionVector.arg,
              directionVector.arg,
              me.speed,
              directionVector.modulus > Boss101.meleeRange
            )

          }

        maybeMoveAction
          .filter(action => action.moving || action.moving != me.moving || (action.position - me.pos).modulus > 1e-6)
          .foreach { action =>
            actionTranslator ! ActionTranslator.GameActionsWrapper(action :: Nil)
          }

        val timeTaken = now - startTime
        AIControllerMessage.unsafeRunSendMeLoop(
          context.self,
          zio.duration.Duration.fromScala(
            ((loopRate - timeTaken) max 0).millis
          )
        )

        Behaviors.same
    }
  }

}
