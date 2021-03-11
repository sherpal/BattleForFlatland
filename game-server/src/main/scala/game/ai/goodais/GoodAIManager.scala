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
import game.ai.goodais.boss.GoodAICreator

import scala.concurrent.duration._
import gamelogic.docs.BossMetadata

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

  /** Sent by the [[game.ActionUpdateCollector]] when it received that information from the [[game.GameMaster]]. */
  case class EntityIdsWithNames(idsAndNames: List[(Entity.Id, PlayerName.AIPlayerName)]) extends Command

  /** Sent internally when a child died (will probably be soon followed by many.) */
  private case class ThisChildDied(ref: ActorRef[Nothing]) extends Command

  /**
    * This class carries all the information the `receiver` needs.
    * Changing the receiver can thus be done much more easily.
    *
    * This class has facility methods for updating its content, all of which using the underlying `copy` method under
    * the hood. This allows to add more semantic in the receiver implementation.
    */
  type GiveGameState = GameState => Unit
  private case class ReceiverInfo(
      maybeGameConfiguration: Option[GameConfiguration],
      actionTranslator: ActorRef[ActionTranslator.Message],
      lastGameState: GameState,
      onlyObstaclesPathFinders: Map[Double, ActorRef[PathFinder.Message]],
      gameStateUpdates: Map[ActorRef[Nothing], GiveGameState]
  ) {
    def withGameState(gameState: GameState): ReceiverInfo = copy(lastGameState = gameState)

    def withGameConfiguration(gameConfiguration: GameConfiguration): ReceiverInfo =
      copy(maybeGameConfiguration = Some(gameConfiguration))

    def withAIRef(ref: ActorRef[Nothing], giveGameState: GiveGameState): ReceiverInfo =
      copy(gameStateUpdates = gameStateUpdates + (ref -> giveGameState))

    def removeAIRef(ref: ActorRef[Nothing]): ReceiverInfo =
      copy(gameStateUpdates = gameStateUpdates - ref)

    def sendGameState(gameState: GameState): Unit = gameStateUpdates.values.foreach(_(gameState))
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
          ),
          Map.empty
        )
      )
    }

  private def receiver(receiverInfo: ReceiverInfo): Behavior[Command] = Behaviors.receive { (context, command) =>
    command match {
      case HereIsTheGameState(gameState) =>
        receiverInfo.sendGameState(gameState)
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
            context.log.info(s"Received entity ids and names ${idsAndNames.mkString(", ")}, creating them...")
            val maybeBossMetadata = for {
              bossName <- config.maybeBossName
              metadata <- BossMetadata.maybeMetadataByName(bossName)
            } yield metadata
            maybeBossMetadata match {
              case Some(metadata) =>
                val children: List[(ActorRef[Nothing], GiveGameState)] =
                  idsAndNames.flatMap {
                    case (id, name) =>
                      for {
                        creator <- GoodAICreator.maybeCreatorByBossMetadata(metadata, name)
                        ref           = context.spawn(creator(id, receiverInfo.actionTranslator), name.name)
                        giveGameState = (gameState: GameState) => ref ! creator.gameStateWrapper(gameState)
                      } yield (ref, giveGameState)

                  }

                context.log.info(s"${children.length} AIs were created")
                if (children.length < idsAndNames.length) {

                  context.log.warn(
                    s"Some ais for ${metadata.name} were not created. This probably means that they are not implemented."
                  )
                }

                children.map(_._1).foreach(ref => context.watchWith(ref, ThisChildDied(ref)))

                val newReceiverInfo =
                  children.foldLeft(receiverInfo)((info, child) => info.withAIRef(child._1, child._2))

                receiver(newReceiverInfo)
              case None =>
                context.log.error(s"There is no metadata for that boss name ${config.maybeBossName}.")
                Behaviors.stopped
            }

          case None =>
            context.log.info("I don't have the config yet, retrying in 1s")
            context.scheduleOnce(1.second, context.self, cmd)
            Behaviors.same
        }

      case ThisChildDied(ref) =>
        receiver(receiverInfo.removeAIRef(ref))
    }
  }

}
