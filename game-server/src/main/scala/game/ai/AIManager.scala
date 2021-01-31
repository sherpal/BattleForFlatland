package game.ai

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import game.ActionTranslator
import game.ai.boss.boss102units.BossHoundController
import game.ai.boss.{Boss101Controller, Boss102Controller, Boss103Controller}
import game.ai.utils.pathfinders.{OnlyObstaclesPathFinder, PathFinder}
import gamelogic.entities.boss.Boss101
import gamelogic.entities.boss.dawnoftime.{Boss102, Boss103}
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.gamestate.gameactions.{AddDummyMob, CreateObstacle, SpawnBoss}
import gamelogic.gamestate.{GameAction, GameState}

/**
  * The [[game.ai.AIManager]] is responsible for spawning and removing "artificial intelligence" actors that will
  * control game entities, outside of players.
  *
  * The idea is that each [[gamelogic.entities.Entity]] (that must take actions, so not a
  * [[gamelogic.entities.SimpleBulletBody]], for example) will be handled by a different actor. If some entities have
  * a very similar behaviour, or if they need to be coordinated, they could be handled together by a single actor.
  *
  * AI actors will typically run at around 30 fps, which is way more than enough to have a meaningful behaviour.
  *
  * The AI manager will receive the [[gamelogic.gamestate.GameState]] computed by the [[game.GameMaster]], and will
  * dispatch the information to every AI actor there is.
  */
object AIManager {

  /** in millis */
  final val loopRate = 1000L / 30L

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

  def apply(actionTranslator: ActorRef[ActionTranslator.Message]): Behavior[Message] = Behaviors.setup { context =>
    receiver(
      ReceiverInfo(
        actionTranslator,
        GameState.empty, // dummy game state when initialized
        Set.empty,
        Map(
          Constants.playerRadius -> context.spawn(
            new OnlyObstaclesPathFinder(Constants.playerRadius)(GameState.empty),
            "OnlyObstaclesPathFinderPlayerRadius"
          ),
          Constants.bossRadius -> context.spawn(
            new OnlyObstaclesPathFinder(Constants.bossRadius)(GameState.empty),
            "OnlyObstaclesPathFinderBossRadius"
          )
        )
      )
    )
  }

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
      entityControllers: Set[ActorRef[AIControllerMessage]],
      onlyObstaclesPathFinders: Map[Double, ActorRef[PathFinder.Message]]
  ) {

    def withGameState(gameState: GameState): ReceiverInfo = copy(lastGameState = gameState)

    def addEntityControllers(newEntityControllers: Iterable[ActorRef[AIControllerMessage]]): ReceiverInfo =
      copy(entityControllers = newEntityControllers.toSet ++ entityControllers)

    def removeEntityController(ref: ActorRef[Nothing]): ReceiverInfo =
      copy(entityControllers = entityControllers - ref.unsafeUpcast[AIControllerMessage])

    def removeAllEntityControllers: ReceiverInfo =
      copy(entityControllers = Set.empty)

  }

  private def receiver(
      receiverInfo: ReceiverInfo
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    def broadcastMessage(message: AIControllerMessage): Unit =
      receiverInfo.entityControllers.foreach(_ ! message)

    message match {
      case HereIsTheGameState(gameState) if gameState.ended =>
        context.log.info("Game has ended, killing all AIs")
        receiverInfo.entityControllers.foreach(context.stop)
        receiver(receiverInfo.removeAllEntityControllers.withGameState(gameState))
      case HereIsTheGameState(gameState) =>
        broadcastMessage(AIControllerMessage.GameStateWrapper(gameState))

        receiver(receiverInfo.withGameState(gameState))
      case HereAreNewActions(newActions, idsToRemove) =>
        val unRemovedActions = newActions
          .filterNot(actions => idsToRemove.contains(actions.id))

        receiverInfo.onlyObstaclesPathFinders.valuesIterator.foreach(
          _ ! PathFinder
            .GameActionsWrapper(unRemovedActions, receiverInfo.lastGameState)
        )

        val newEntityControllers = unRemovedActions.collect {
          case action: AddDummyMob =>
            val ref =
              context.spawn(
                DummyMobController(receiverInfo.actionTranslator, action),
                s"DummyMob-${action.entityId}"
              )

            context.watchWith(ref, ControllerDied(ref))

            ref
          case action: SpawnBoss if action.bossName == Boss101.name =>
            val ref = context.spawn(
              Boss101Controller(receiverInfo.actionTranslator, action),
              s"Boss101-${action.entityId}"
            )
            context.watchWith(ref, ControllerDied(ref))
            ref
          case action: SpawnBoss if action.bossName == Boss102.name =>
            val ref = context.spawn(
              Boss102Controller.apply(
                receiverInfo.actionTranslator,
                action,
                receiverInfo.onlyObstaclesPathFinders(Constants.bossRadius)
              ),
              s"Boss102-${action.entityId}"
            )
            context.watchWith(ref, ControllerDied(ref))
            ref
          case action: SpawnBoss if action.bossName == Boss103.name =>
            val ref = context.spawn(
              Boss103Controller.apply(
                receiverInfo.actionTranslator,
                action,
                receiverInfo.onlyObstaclesPathFinders(Constants.bossRadius)
              ),
              s"Boss103-${action.entityId}"
            )
            context.watchWith(ref, ControllerDied(ref))
            ref
          case action: AddBossHound =>
            val ref = context.spawn(
              BossHoundController.apply(
                receiverInfo.actionTranslator,
                action,
                receiverInfo.onlyObstaclesPathFinders(Constants.playerRadius)
              ),
              s"BossHound-${action.entityId}"
            )
            context.watchWith(ref, ControllerDied(ref))
            ref
        }.toSet

        newEntityControllers.foreach(_ ! AIControllerMessage.GameStateWrapper(receiverInfo.lastGameState))
        if (newActions.nonEmpty) {
          broadcastMessage(AIControllerMessage.NewActions(unRemovedActions))
        }

        if (newEntityControllers.isEmpty) Behaviors.same
        else receiver(receiverInfo.addEntityControllers(newEntityControllers))

      case ControllerDied(ref) =>
        receiver(receiverInfo.removeEntityController(ref))
    }
  }

}
