package game.ai.boss

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.AIControllerMessage
import game.ai.AIManager.loopRate
import game.ai.utils.findTarget
import game.ai.utils.pathfinders.PathFinder
import gamelogic.entities.classes.PlayerClass
import gamelogic.entities.{Entity, MovingBody, WithPosition, WithThreat}
import gamelogic.gamestate.GameAction.EntityCreatorAction
import gamelogic.gamestate.gameactions.{ChangeTarget, EntityStartsCasting, MovingBodyMoves}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.concurrent.duration._
import gamelogic.abilities.Ability

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
    InitialAction <: EntityCreatorAction
] {

  protected implicit class StartCasting(maybeAbility: Option[Ability]) {
    def startCasting: Option[EntityStartsCasting] = maybeAbility.map(
      ability => EntityStartsCasting(0L, ability.time, ability.castingTime, ability)
    )
  }

  protected def takeActions(
      currentGameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      lastTimeStamp: Long,
      maybeTarget: Option[PlayerClass],
      obstacleGraph: Graph
  ): List[GameAction]

  protected def getMe(gameState: GameState, entityId: Entity.Id): Option[EntityType]

  @inline private def now = System.currentTimeMillis

  def apply(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialMessage: InitialAction,
      pathFinder: ActorRef[PathFinder.Message]
  ): Behavior[AIControllerMessage] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case AIControllerMessage.GameStateWrapper(gameState) =>
        AIControllerMessage.unsafeRunSendMeLoop(context.self, zio.duration.Duration.fromScala(loopRate.millis))
        pathFinder ! PathFinder.NewSubscriber(context.self)
        waitingForGraph(actionTranslator, initialMessage = initialMessage, gameState = gameState)
      case _ =>
        //waiting for first game state
        Behaviors.same
    }
  }

  private def waitingForGraph(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialMessage: InitialAction,
      gameState: GameState
  ): Behavior[AIControllerMessage] = Behaviors.receiveMessage {
    case AIControllerMessage.GameStateWrapper(newGameState) =>
      waitingForGraph(actionTranslator, initialMessage, newGameState)
    case AIControllerMessage.ObstacleGraph(graph) =>
      receiver(actionTranslator, initialMessage, gameState, graph, now)
    case _ => Behaviors.unhandled
  }

  private def receiver(
      actionTranslator: ActorRef[ActionTranslator.Message],
      initialAction: InitialAction,
      currentGameState: GameState,
      obstacleGraph: Graph,
      lastTimeStamp: Long
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
                gameState,
                obstacleGraph,
                lastTimeStamp
              )
          )
      case AIControllerMessage.NewActions(_) => Behaviors.same
      case AIControllerMessage.Loop if currentGameState.ended =>
        println("Game has ended, I stop doing stuff")
        Behaviors.same
      case AIControllerMessage.Loop =>
        val startTime       = now
        val me              = getMe(currentGameState, myId).get
        val currentPosition = me.currentPosition(startTime)

        val maybeTarget = findTarget(me, currentGameState)

        val actions =
          takeActions(currentGameState, me, currentPosition, startTime, lastTimeStamp, maybeTarget, obstacleGraph)

        Option
          .unless(actions.isEmpty)(ActionTranslator.GameActionsWrapper(actions))
          .foreach(actionTranslator ! _)

        val timeTaken = now - startTime
        AIControllerMessage.unsafeRunSendMeLoop(
          context.self,
          zio.duration.Duration.fromScala(
            ((loopRate - timeTaken) max 0).millis
          )
        )
        receiver(actionTranslator, initialAction, currentGameState, obstacleGraph, startTime)
      case AIControllerMessage.ObstacleGraph(graph) =>
        receiver(actionTranslator, initialAction, currentGameState, graph, lastTimeStamp)
    }
  }

  /**
    * Utility method usable by sub classes which will (maybe) determine what action(s) need to be performed.
    *
    * @param maybeActions list of legal attack that could be used at the given time. The attack, if any, which will be
    *                     performed correspond to the first defined element in the list.
    * @param maybeChangeTarget if the entity needs to change its target, this argument must be defined
    * @param maybeMove if the entity must move, this argument must be defined
    * @return
    */
  protected def useAbility(
      maybeActions: List[Option[EntityStartsCasting]],
      maybeChangeTarget: Option[ChangeTarget],
      maybeMove: Option[MovingBodyMoves]
  ): List[GameAction] = {
    val maybeAction = maybeActions.collectFirst { case Some(action) => action }
    maybeAction.fold(
      List(maybeChangeTarget, maybeMove).flatten
    )(
      _ =>
        List(
          maybeChangeTarget,
          maybeMove.map(_.copy(moving = false)),
          maybeAction
        ).flatten
    )
  }

}
