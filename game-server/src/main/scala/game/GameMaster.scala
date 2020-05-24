package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.entities.classes.PlayerClassBuilder
import gamelogic.gamestate.gameactions.{AddPlayerByClass, EntityStartsCasting, GameStart, SpawnBoss, UseAbility}
import gamelogic.gamestate.serveractions._
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._

object GameMaster {

  sealed trait Message

  sealed trait InGameMessage extends Message

  /** Sent by this actor to itself to run the game loop. We try to keep a rate of 120 per second. */
  private case object GameLoop extends InGameMessage

  /** A single action was sent by a game entity. We add it to the queue to be processed during the game loop */
  case class GameActionWrapper(gameAction: GameAction) extends InGameMessage

  /** Same as [[GameActionWrapper]] but for multiple actions. */
  case class MultipleActionsWrapper(gameActions: List[GameAction]) extends InGameMessage

  sealed trait PreGameMessage extends Message

  /**
    * Sent by the [[game.AntiChamber]] when every client is ready.
    * The map is used so that the game master can tell the client what entity id they have at the beginning of the game.
    * @param playerMap map from the userId to the client actor
    * @param gameInfo information of the game, in order to know what kind of game to create, and what to put in it.
    */
  case class EveryoneIsReady(playerMap: Map[String, ActorRef[InGameWSProtocol]], gameInfo: MenuGameWithPlayers)
      extends PreGameMessage

  /**
    * Once every one is connected and this game master received the `EveryoneIsReady` message, it sends their entity id
    * to each client.
    * Each client will then do stuff accordingly, and send this message back to the game master in order to actually
    * start the game.
    */
  case class IAmReadyToStart(userId: String) extends PreGameMessage

  private def now = System.currentTimeMillis

  private def gameLoopTo(to: ActorRef[GameLoop.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _ <- fiber.join
      _ <- ZIO.effectTotal(to ! GameLoop)
    } yield ()

//  private def spawnMobLoop(
//      to: ActorRef[GameActionWrapper],
//      each: FiniteDuration,
//      entityIdGenerator: EntityIdGenerator
//  ) =
//    (for {
//      fiber <- zio.clock.sleep(fromScala(each)).fork
//      _ <- fiber.join
//      real <- zio.random.nextGaussian
//      imag <- zio.random.nextGaussian
//      pos = 100 * Complex(real, imag)
//      _ <- ZIO.effectTotal(to ! GameActionWrapper(AddDummyMob(0L, now, entityIdGenerator(), pos)))
//    } yield ()).forever

  // todo: add other server actions.
  private val serverAction = new ManageUsedAbilities ++
    new ManageStopCastingMovements ++
    new ManageTickerBuffs ++
    new ManageBuffsToBeRemoved ++
    new ManageDeadPlayers ++
    new ManageEndOfGame

  def apply(actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]): Behavior[Message] =
    setupBehaviour(actionUpdateCollector, None, Set.empty, None)(IdGeneratorContainer.initialIdGeneratorContainer)

  def inGameBehaviour(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      actionCollector: ImmutableActionCollector
  )(
      implicit
      idGeneratorContainer: IdGeneratorContainer
  ): Behavior[Message] = Behaviors.setup { implicit context =>
    Behaviors
      .receiveMessage[Message] {
        case GameActionWrapper(gameAction) =>
          // todo: We should check the minimal legality of actions here. That is, a position update of an entity
          // todo: should at least check that it is the given entity that sent the message.

          // Note: we sort actions during the game loop so we can simply prepend and enjoy the O(1) complexity.
          inGameBehaviour(
            gameAction +: pendingActions,
            actionUpdateCollector,
            actionCollector
          )
        case MultipleActionsWrapper(gameActions) =>
          inGameBehaviour(
            gameActions ++ pendingActions,
            actionUpdateCollector,
            actionCollector
          )
        case GameLoop if actionCollector.currentGameState.ended =>
          Behaviors.stopped
        case GameLoop =>
          val startTime = now
          val sortedActions = pendingActions.sorted
            .map(_.changeId(idGeneratorContainer.gameActionIdGenerator()))

          /** First adding actions from entities */
          val (nextCollector, oldestTimeToRemove, idsToRemove) =
            actionCollector.masterAddAndRemoveActions(sortedActions)

          /** Making all the server specific checks */
          val (finalCollector, output) = serverAction(
            nextCollector,
            () => System.currentTimeMillis
          )

          /** Sending outcome back to entities. */
          val finalOutput = ServerAction.ServerActionOutput(
            sortedActions,
            oldestTimeToRemove,
            idsToRemove
          ) merge output
          if (finalOutput.createdActions.nonEmpty) {
            actionUpdateCollector ! ActionUpdateCollector
              .AddAndRemoveActions(
                finalOutput.createdActions,
                finalOutput.oldestTimeToRemove,
                finalOutput.idsOfIdsToRemove
              )

            actionUpdateCollector ! ActionUpdateCollector.GameStateWrapper(finalCollector.currentGameState)
          }

          /** Set up for next loop. */
          val timeSpent = now - startTime
          if (timeSpent > gameLoopTiming) context.self ! GameLoop
          else
            zio.Runtime.default
              .unsafeRunToFuture(
                gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
              )

          inGameBehaviour(Nil, actionUpdateCollector, finalCollector)

        case _: PreGameMessage => Behaviors.unhandled
      }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 60L

  private def setupBehaviour(
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      maybeGameInfo: Option[MenuGameWithPlayers],
      readyPlayers: Set[String], // set of user ids that are now ready.
      maybePreGameActions: Option[List[GameAction]]
  )(implicit idGeneratorContainer: IdGeneratorContainer): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case message: PreGameMessage =>
          message match {
            case EveryoneIsReady(playerMap, gameInfo) =>
              // first message that kick off the actor and will trigger others
              val timeNow = now

              try {
                val refsByName = gameInfo.players.map(user => user.userName -> playerMap(user.userId)).toMap

                val newPlayerActions = gameInfo.game.gameConfiguration.playersInfo.values.zipWithIndex.map {
                  case (info, idx) =>
                    val playerId = idGeneratorContainer.entityIdGenerator()
                    (
                      refsByName(info.playerName),
                      playerId,
                      AddPlayerByClass(
                        0L,
                        timeNow - 2,
                        playerId,
                        100 * Complex.rotation(idx * 2 * math.Pi / playerMap.size),
                        info.playerClass,
                        info.playerColour.intColour,
                        info.playerName
                      ) +: info.playerClass.builder.startingActions(timeNow - 1, playerId, idGeneratorContainer)
                    )

                }

                if (gameInfo.game.gameConfiguration.maybeBossName.isEmpty) {
                  println("Starting Game without boss, that's weird.")
                }

                val bossCreationActions = gameInfo.game.gameConfiguration.maybeBossName.toList.map(
                  SpawnBoss(0L, timeNow - 2, idGeneratorContainer.entityIdGenerator(), _)
                )

                newPlayerActions.foreach {
                  case (ref, playerId, _) =>
                    ref ! InGameWSProtocol.YourEntityIdIs(playerId)
                }

                setupBehaviour(
                  actionUpdateCollector,
                  Some(gameInfo),
                  Set(),
                  Some(newPlayerActions.flatMap(_._3).toList ++ bossCreationActions :+ GameStart(0L, now))
                )

              } catch {
                case e: Throwable =>
                  e.printStackTrace()
                  throw e
              }

            case IAmReadyToStart(userId) =>
              val newReadyPlayers = readyPlayers + userId

              if (maybeGameInfo.map(_.players.length).contains(newReadyPlayers.size)) {

                context.self ! MultipleActionsWrapper(maybePreGameActions.get)
                context.self ! GameLoop

                val actionCollector = ImmutableActionCollector(GameState.empty)
                inGameBehaviour(
                  Nil,
                  actionUpdateCollector,
                  actionCollector
                )
              } else {
                setupBehaviour(actionUpdateCollector, maybeGameInfo, newReadyPlayers, maybePreGameActions)
              }
          }

        case _ =>
          Behaviors.unhandled
      }
    }

}
