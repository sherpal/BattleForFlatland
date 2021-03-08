package game.ai.goodais

import akka.actor.typed.ActorRef
import gamelogic.gamestate.GameState
import game.ActionTranslator
import game.ai.utils.pathfinders.PathFinder
import gamelogic.gamestate.GameAction
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import gamelogic.entities.classes.Constants
import game.ai.utils.pathfinders.OnlyObstaclesPathFinder
import models.bff.outofgame.gameconfig.GameConfiguration
import gamelogic.entities.Entity
import models.bff.outofgame.gameconfig.PlayerName

import scala.concurrent.duration._

object GoodAIManager {

  /** in millis */
  final val loopRate = 1000L / 30L

  sealed trait Command

  /**
    * Refreshes the current game state that the AI manager knows about.
    * It will be in charge of telling all the AI about it.
    * We keep a reference to it so that newly created ai actors can have it immediately.
    *
    * Note: it is the responsibility of the [[GoodAIManager]] to be sure that each AI actor has the proper game state
    * information.
    */
  case class HereIsTheGameState(gameState: GameState) extends Command

  /**
    * These are the new actions that the server treated in its last iteration.
    * We also receive the ids of the actions that have been removed, so that we can immediately remove from the
    * `newAction` list the actions that have been removed.
    *
    * The AIManager will treat this message by spawning or deleting any actor that was affected by these actions.
    */
  case class HereAreNewActions(newActions: List[GameAction], idsToRemove: List[Long]) extends Command

  /**
    * Sent to this actor in order to tell what is the [[GameConfiguration]]
    */
  case class HereIsTheGameConfiguration(config: GameConfiguration) extends Command

  case class EntityIdsWithNames(idsAndNames: List[(Entity.Id, PlayerName.AIPlayerName)]) extends Command

  /**
    * This class carries all the information the `receiver` needs.
    * Changing the receiver can thus be done much more easily.
    *
    * This class has facility methods for updating its content, all of which using the underlying `copy` method under
    * the hood. This allows to add more semantic in the receiver implementation.
    */
  private case class ReceiverInfo(
      maybeGameConfiguration: Option[GameConfiguration],
      actionTranslator: ActorRef[ActionTranslator.Message],
      lastGameState: GameState,
      onlyObstaclesPathFinders: Map[Double, ActorRef[PathFinder.Message]]
  ) {
    def withGameState(gameState: GameState): ReceiverInfo = copy(lastGameState = gameState)

    def withGameConfiguration(gameConfiguration: GameConfiguration): ReceiverInfo =
      copy(maybeGameConfiguration = Some(gameConfiguration))

    // todo
    def withAIsRefs(): ReceiverInfo = ???
  }

  def apply(actionTranslator: ActorRef[ActionTranslator.Message]): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info("Good AI Manager launched.")
      receiver(
        ReceiverInfo(
          Option.empty,
          actionTranslator,
          GameState.empty,
          Map(
            Constants.playerRadius -> context.spawn(
              new OnlyObstaclesPathFinder(Constants.playerRadius)(GameState.empty),
              "OnlyObstaclesPathFinderPlayerRadius"
            )
          )
        )
      )
    }

  private def receiver(receiverInfo: ReceiverInfo): Behavior[Command] = Behaviors.receive { (context, command) =>
    command match {
      case HereIsTheGameState(gameState) =>
        // todo: send to children
        receiver(receiverInfo.withGameState(gameState))
      case HereAreNewActions(newActions, idsToRemove) =>
        // we actually do nothing here
        Behaviors.same
      case HereIsTheGameConfiguration(config) =>
        config.maybeBossName match {
          case Some(bossName) =>
            receiver(receiverInfo.withGameConfiguration(config))
          case None =>
            context.log.warn("Received a config without boss. There is nothing I can do.")
            Behaviors.stopped
        }
      case cmd @ EntityIdsWithNames(idsAndNames) =>
        receiverInfo.maybeGameConfiguration match {
          case Some(config) =>
            context.log.info("Received entity ids and names, creating them...")
            // todo
            receiver(receiverInfo.withAIsRefs())
          case None =>
            context.log.info("I don't have the config yet, retrying in 1s")
            context.scheduleOnce(1.second, context.self, cmd)
            Behaviors.same
        }
    }
  }

}
