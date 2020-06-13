package game.ai.boss

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIControllerMessage
import game.ai.AIManager.loopRate
import game.ai.utils.findTarget
import gamelogic.entities.boss.BossEntity
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.gameactions.SpawnBoss
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

import scala.concurrent.duration._

/**
  * The BossController trait contains all the boilerplate that you need for implementing an AI for a given boss.
  *
  * The only method to implement is the `takeActions` method. It takes the current [[gamelogic.gamestate.GameState]]
  * and a few pre-computed values. And it returns the list of actions that the boss wants to take given these inputs.
  */
trait BossController {

  protected def takeActions(
      currentGameState: GameState,
      me: BossEntity,
      currentPosition: Complex,
      startTime: Long,
      maybeTarget: Option[PlayerClass]
  ): List[GameAction]

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
        val currentPosition = me.lastValidPosition(me.currentPosition(startTime), currentGameState.obstaclesLike.toList)

        val maybeTarget = findTarget(me, currentGameState)

        val actions = takeActions(currentGameState, me, currentPosition, startTime, maybeTarget)

        if (actions.nonEmpty) {
          actionTranslator ! ActionTranslator.GameActionsWrapper(actions)
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
