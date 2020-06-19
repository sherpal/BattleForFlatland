package game.ai.boss

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIControllerMessage
import game.ai.AIManager.loopRate
import game.ai.utils.findTarget
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, MovingBody, WithPosition, WithThreat}
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex

import scala.concurrent.duration._

/**
  * The AIController trait contains all the boilerplate that you need for implementing an AI.
  *
  * The methods to implement are
  * - `takeActions` method. It takes the current [[gamelogic.gamestate.GameState]]
  *   and a few pre-computed values. And it returns the list of actions that the AI wants to take given these inputs.
  * - `getMe`: how to access this entity based on its id
  */
trait AIController[
    EntityType <: MovingBody with WithThreat with WithPosition,
    InitialAction <: GameAction with EntityCreatorAction
] {

  protected def takeActions(
      currentGameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      maybeTarget: Option[PlayerClass]
  ): List[GameAction]

  protected def getMe(gameState: GameState, entityId: Entity.Id): Option[EntityType]

  @inline private def now = System.currentTimeMillis

  def apply(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialMessage: InitialAction
  ): Behavior[AIControllerMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        AIControllerMessage.unsafeRunSendMeLoop(context.self, zio.duration.Duration.fromScala(loopRate.millis))
        receiver(actionTranslator, initialAction = initialMessage, currentGameState = gameState)
      case _ =>
        //waiting for first game state
        Behaviors.same
    }
  }

  private def receiver(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialAction: InitialAction,
      currentGameState: GameState
  ): Behavior[AIControllerMessage] = Behaviors.receive { (context, message) =>
    val myId = initialAction.entityId

    message match {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        getMe(gameState, myId)
          .fold(Behaviors.stopped[AIControllerMessage])(
            _ =>
              receiver(
                actionTranslator,
                initialAction,
                gameState
              )
          )
      case AIControllerMessage.NewActions(_) => Behaviors.same
      case AIControllerMessage.Loop =>
        val startTime       = now
        val me              = getMe(currentGameState, myId).get
        val currentPosition = me.currentPosition(startTime)
        //me.lastValidPosition(me.currentPosition(startTime), currentGameState.obstaclesLike.toList)

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
