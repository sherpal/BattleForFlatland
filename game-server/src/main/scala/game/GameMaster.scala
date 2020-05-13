package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.gamestate.gameactions.{AddDummyMob, AddPlayer, GameStart}
import gamelogic.gamestate.serveractions.{ManageStopCastingMovements, ManageUsedAbilities, ServerAction}
import gamelogic.gamestate.{GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import gamelogic.utils.{AbilityUseIdGenerator, EntityIdGenerator, GameActionIdGenerator}
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._
import scala.util.Random

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

  private def spawnMobLoop(
      to: ActorRef[GameActionWrapper],
      each: FiniteDuration,
      entityIdGenerator: EntityIdGenerator
  ) =
    (for {
      fiber <- zio.clock.sleep(fromScala(each)).fork
      _ <- fiber.join
      real <- zio.random.nextGaussian
      imag <- zio.random.nextGaussian
      pos = 100 * Complex(real, imag)
      _ <- ZIO.effectTotal(to ! GameActionWrapper(AddDummyMob(0L, now, entityIdGenerator(), pos)))
    } yield ()).forever

  // todo: add other server actions.
  private val serverAction = new ManageUsedAbilities ++ new ManageStopCastingMovements

  def apply(actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]): Behavior[Message] =
    setupBehaviour(actionUpdateCollector, None, Set.empty, None)

  def inGameBehaviour(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      actionCollector: ImmutableActionCollector,
      gameActionIdGenerator: GameActionIdGenerator,
      entityIdGenerator: EntityIdGenerator,
      abilityUseIdGenerator: AbilityUseIdGenerator
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
            actionCollector,
            gameActionIdGenerator,
            entityIdGenerator,
            abilityUseIdGenerator
          )
        case MultipleActionsWrapper(gameActions) =>
          inGameBehaviour(
            gameActions ++ pendingActions,
            actionUpdateCollector,
            actionCollector,
            gameActionIdGenerator,
            entityIdGenerator,
            abilityUseIdGenerator
          )
        case GameLoop =>
          val startTime = now
          val sortedActions = pendingActions.sorted
            .map(_.changeId(gameActionIdGenerator()))

          /** First adding actions from entities */
          val (nextCollector, oldestTimeToRemove, idsToRemove) =
            actionCollector.masterAddAndRemoveActions(sortedActions)

          /** Making all the server specific checks */
          val (finalCollector, output) = serverAction(
            nextCollector,
            gameActionIdGenerator,
            entityIdGenerator,
            abilityUseIdGenerator,
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

          inGameBehaviour(
            Nil,
            actionUpdateCollector,
            finalCollector,
            gameActionIdGenerator,
            entityIdGenerator,
            abilityUseIdGenerator
          )

        case _: PreGameMessage => Behaviors.unhandled
      }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 100L

  private def setupBehaviour(
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      maybeGameInfo: Option[MenuGameWithPlayers],
      readyPlayers: Set[String], // set of user ids that are now ready.
      maybePreGameActions: Option[List[GameAction]]
  ): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case message: PreGameMessage =>
          message match {
            case EveryoneIsReady(playerMap, gameInfo) =>
              // first message that kick off the actor and will trigger others
              val n = now

              val newPlayerActions = playerMap.values.zipWithIndex.map {
                case (ref, idx) =>
                  ref -> AddPlayer(
                    0,
                    n - 1,
                    idx.toLong,
                    100 * Complex.rotation(idx * 2 * math.Pi / playerMap.size),
                    Random.nextInt(0xFFFFFF)
                  )
              }

              newPlayerActions.foreach {
                case (ref, player) =>
                  ref ! InGameWSProtocol.YourEntityIdIs(player.playerId)
              }

              setupBehaviour(
                actionUpdateCollector,
                Some(gameInfo),
                Set(),
                Some(newPlayerActions.map(_._2).toList :+ GameStart(0L, now))
              )

            case IAmReadyToStart(userId) =>
              val newReadyPlayers = readyPlayers + userId

              if (maybeGameInfo.map(_.players.length).contains(newReadyPlayers.size)) {

                context.self ! MultipleActionsWrapper(maybePreGameActions.get)
                context.self ! GameLoop

                val entityIdGenerator = new EntityIdGenerator(readyPlayers.size)

                zio.Runtime.default.unsafeRunAsync(spawnMobLoop(context.self, 5.seconds, entityIdGenerator))(println(_))

                val actionCollector = ImmutableActionCollector(GameState.empty)
                inGameBehaviour(
                  Nil,
                  actionUpdateCollector,
                  actionCollector,
                  new GameActionIdGenerator(0L),
                  entityIdGenerator,
                  new AbilityUseIdGenerator(0L)
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
