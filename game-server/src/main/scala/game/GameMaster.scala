package game

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gamelogic.entities.boss.BossFactory
import gamelogic.gamestate.gameactions.{AddPlayerByClass, GameStart, MovingBodyMoves, SpawnBoss, UpdateTimestamp}
import gamelogic.gamestate.serveractions._
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, ImmutableActionCollector}
import gamelogic.physics.Complex
import gamelogic.utils.IdGeneratorContainer
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.MenuGameWithPlayers
import models.bff.outofgame.gameconfig.PlayerName
import models.bff.outofgame.gameconfig.PlayerName.{AIPlayerName, HumanPlayerName}
import zio.ZIO
import zio.duration.Duration.fromScala

import scala.concurrent.duration._
import gamelogic.gamestate.GreedyActionGatherer

object GameMaster {

  sealed trait Message

  sealed trait InGameMessage extends Message

  /** Sent by this actor to itself to run the game loop. We try to keep a rate of 120 per second. */
  private case object GameLoop extends InGameMessage

  /** A single action was sent by a game entity. We add it to the queue to be processed during the game loop */
  case class GameActionWrapper(gameAction: GameAction) extends InGameMessage

  /** Same as [[GameActionWrapper]] but for multiple actions. */
  case class MultipleActionsWrapper(gameActions: List[GameAction]) extends InGameMessage

  /** Sent to itself one minute after the game has ended in order to close the server. */
  private case object Close extends InGameMessage

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

  /**
    * When players join the game, the boss is not there yet. Players will have the opportunity to send this message to
    * the game master in order to actually begin the game, when they are ready to do so.
    *
    * This will allow players to position themselves properly in order to be ready.
    */
  case object LetsStartTheGame extends PreGameMessage

  private def now = System.currentTimeMillis

  private def gameLoopTo(to: ActorRef[GameLoop.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _     <- fiber.join
      _     <- ZIO.effectTotal(to ! GameLoop)
    } yield ()

  private def closeServerAfter(to: ActorRef[Close.type], delay: FiniteDuration) =
    for {
      fiber <- zio.clock.sleep(fromScala(delay)).fork
      _     <- fiber.join
      _     <- ZIO.effectTotal(to ! Close)
    } yield ()

  // todo: add other server actions.
  private val serverAction = new ManageUsedAbilities ++
    new ManageStopCastingMovements ++
    new ManageTickerBuffs ++
    new ManageBuffsToBeRemoved ++
    new ManageDeadPlayers ++
    new ManageEndOfGame ++
    new ManagePentagonBullets ++
    new ManageDeadAIs

  def apply(actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage]): Behavior[Message] =
    setupBehaviour(actionUpdateCollector, None, Set.empty, None)(IdGeneratorContainer.initialIdGeneratorContainer)

  def inGameBehaviour(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      actionCollector: ActionGatherer,
      bossFactory: List[BossFactory[_]],
      alreadyClosing: Boolean = false
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
            actionCollector,
            bossFactory
          )
        case MultipleActionsWrapper(gameActions) =>
          inGameBehaviour(
            gameActions ++ pendingActions,
            actionUpdateCollector,
            actionCollector,
            bossFactory
          )
        case GameLoop =>
          val startTime = now
          val sortedActions = pendingActions.sorted
            .map(_.changeId(idGeneratorContainer.gameActionIdGenerator()))

          try {

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
              actionUpdateCollector ! ActionUpdateCollector.GameStateWrapper(finalCollector.currentGameState)
              actionUpdateCollector ! ActionUpdateCollector
                .AddAndRemoveActions(
                  finalOutput.createdActions,
                  finalOutput.oldestTimeToRemove,
                  finalOutput.idsOfIdsToRemove
                )
            }

            /** Set up for next loop. */
            val timeSpent = now - startTime
            if (timeSpent > gameLoopTiming) context.self ! GameLoop
            else
              zio.Runtime.default
                .unsafeRunToFuture(
                  gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
                )

            if (finalCollector.currentGameState.ended && !alreadyClosing) {
              zio.Runtime.default.unsafeRunToFuture(
                closeServerAfter(context.self, 1.minute)
              )

              val endOfGameActions =
                bossFactory.flatMap(_.whenBossDiesActions(finalCollector.currentGameState, now, idGeneratorContainer))

              val (endOfGameCollector, oldestTimeToRemove, idsToRemove) =
                actionCollector.masterAddAndRemoveActions(endOfGameActions)

              actionUpdateCollector ! ActionUpdateCollector.GameStateWrapper(endOfGameCollector.currentGameState)
              actionUpdateCollector ! ActionUpdateCollector
                .AddAndRemoveActions(
                  endOfGameActions,
                  oldestTimeToRemove,
                  idsToRemove
                )

              inGameBehaviour(Nil, actionUpdateCollector, endOfGameCollector, bossFactory, alreadyClosing = true)
            } else {
              inGameBehaviour(Nil, actionUpdateCollector, finalCollector, bossFactory)
            }

          } catch {
            case e: Throwable =>
              println(e.getStackTrace.toList.mkString("\n"))
              e.printStackTrace()
              throw e
          }

        case Close =>
          println("Closing server...")
          Behaviors.stopped

        case _: PreGameMessage => Behaviors.unhandled
      }

  }

  /** In millis */
  final val gameLoopTiming = 1000L / 30L

  private def preGameBehaviour(
      pendingActions: List[GameAction],
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      actionCollector: ActionGatherer,
      gameInfo: MenuGameWithPlayers
  )(
      implicit
      idGeneratorContainer: IdGeneratorContainer
  ): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case GameActionWrapper(gameAction) =>
        // todo: We should check the minimal legality of actions here. That is, a position update of an entity
        // todo: should at least check that it is the given entity that sent the message.

        // Note: we sort actions during the game loop so we can simply prepend and enjoy the O(1) complexity.
        preGameBehaviour(
          gameAction +: pendingActions,
          actionUpdateCollector,
          actionCollector,
          gameInfo
        )
      case MultipleActionsWrapper(gameActions) =>
        preGameBehaviour(
          gameActions ++ pendingActions,
          actionUpdateCollector,
          actionCollector,
          gameInfo
        )
      case GameLoop =>
        val startTime = now

        /**
          * Adding a [[gamelogic.gamestate.gameactions.UpdateTimestamp]] so that there are actions even if no
          * body does anything. (Otherwise the game can crash at launch) */
        val sortedActions = (UpdateTimestamp(0L, now) +: pendingActions).sorted
          .map(_.changeId(idGeneratorContainer.gameActionIdGenerator()))

        val (nextCollector, oldestTimeToRemove, idsToRemove) =
          actionCollector.masterAddAndRemoveActions(sortedActions)

        val output = ServerAction.ServerActionOutput(
          sortedActions,
          oldestTimeToRemove,
          idsToRemove
        )

        if (output.createdActions.nonEmpty) {

          actionUpdateCollector ! ActionUpdateCollector.GameStateWrapper(nextCollector.currentGameState)
          actionUpdateCollector ! ActionUpdateCollector
            .AddAndRemoveActions(
              output.createdActions,
              output.oldestTimeToRemove,
              output.idsOfIdsToRemove
            )
        }

        /** Set up for next loop. */
        val timeSpent = now - startTime
        if (timeSpent > gameLoopTiming) context.self ! GameLoop
        else
          zio.Runtime.default
            .unsafeRunToFuture(
              gameLoopTo(context.self, (gameLoopTiming - timeSpent).millis)
            )

        preGameBehaviour(Nil, actionUpdateCollector, nextCollector, gameInfo)
      case LetsStartTheGame =>
        val timeNow = now
        val bossCreationActions = gameInfo.game.gameConfiguration.maybeBossName.toList.flatMap { name =>
          val bossId = idGeneratorContainer.entityIdGenerator()
          SpawnBoss(0L, timeNow - 2, bossId, name) +: BossFactory.factoriesByBossName
            .get(name)
            .fold(List.empty[GameAction])(_.initialBossActions(bossId, timeNow - 1, idGeneratorContainer))
        }

        val bossFactories = gameInfo.game.gameConfiguration.maybeBossName.toList.flatMap(
          BossFactory.factoriesByBossName.get(_)
        )

        val newPendingActions = pendingActions ++ bossCreationActions :+ GameStart(0L, now)

        inGameBehaviour(
          newPendingActions,
          actionUpdateCollector,
          actionCollector,
          bossFactories
        )
      case _ =>
        Behaviors.unhandled
    }
  }

  private def setupBehaviour(
      actionUpdateCollector: ActorRef[ActionUpdateCollector.ExternalMessage],
      maybeGameInfo: Option[MenuGameWithPlayers],
      readyPlayers: Set[String], // set of user ids that are now ready.
      maybePreGameActions: Option[List[GameAction]]
  )(implicit idGeneratorContainer: IdGeneratorContainer): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      context.log.info("GameMaster started")
      message match {
        case message: PreGameMessage =>
          message match {
            case LetsStartTheGame =>
              Behaviors.unhandled // we only care about this message when in preGameBehaviour
            case EveryoneIsReady(playerMap, gameInfo) =>
              context.log.info("Everyone is ready")
              // first message that kick off the actor and will trigger others
              val timeNow = now

              gameInfo.game.gameConfiguration.asValid match {
                case None =>
                  context.log.info("""
                      |I received game information with an invalid game configuration.
                      |This should not happen. Something went terribly wrong upstream.
                      |
                      |Shutting down now...
                      |""".stripMargin)
                  Behaviors.stopped
                case Some(gameConfiguration) =>
                  try {

                    def maybeBossFactory = BossFactory.factoriesByBossName.get(gameConfiguration.bossName).toList

                    if (maybeBossFactory.isEmpty) {
                      context.log.warn("Starting Game without boss, that's weird.")
                    }

                    context.log.info("Creating refs by name map.")
                    val refsByName: Map[PlayerName, ActorRef[InGameWSProtocol]] =
                      gameInfo.players.map(user => HumanPlayerName(user.userName) -> playerMap(user.userId)).toMap

                    context.log.info("Computing player positions.")
                    val playersPosition = maybeBossFactory
                      .map(_.playersStartingPosition)
                      .head

                    val bossStartingPosition = maybeBossFactory.headOption.fold(Complex.zero)(_.bossStartingPosition)

                    context.log.info("Creating NewPlayer actions")
                    val newPlayerActions = gameConfiguration.playersInfo.values.map { info =>
                      val playerId = idGeneratorContainer.entityIdGenerator()
                      (
                        refsByName.get(info.playerName),
                        if (info.playerType.playing) playerId else -1,
                        Option.when(info.playerType.playing)(
                          AddPlayerByClass(
                            0L,
                            timeNow - 2,
                            playerId,
                            playersPosition,
                            info.playerClass,
                            info.playerColour.intColour,
                            info.playerName.name
                          ) +: info.playerClass.builder.startingActions(timeNow - 1, playerId, idGeneratorContainer)
                        ),
                        info.playerName
                      )

                    }

                    context.log.info("Creating Boss Creation actions")
                    val bossCreationActions = gameInfo.game.gameConfiguration.maybeBossName.toList.flatMap { name =>
                      BossFactory.factoriesByBossName
                        .get(name)
                        .fold(List.empty[GameAction])(_.stagingBossActions(timeNow, idGeneratorContainer))
                    }

                    context.log.info(s"Boss creation action at $timeNow")

                    newPlayerActions.foreach {
                      case (Some(ref), playerId, _, _) =>
                        ref ! InGameWSProtocol.YourEntityIdIs(playerId)
                        ref ! InGameWSProtocol.StartingBossPosition(bossStartingPosition.re, bossStartingPosition.im)
                      case (None, playerId, _, name) =>
                    }

                    actionUpdateCollector ! ActionUpdateCollector.EntityIdsAndNamesForAIs(
                      newPlayerActions.collect {
                        case (None, playerId, _, AIPlayerName(cls, index)) =>
                          (playerId, AIPlayerName(cls, index))
                      }.toList
                    )

                    setupBehaviour(
                      actionUpdateCollector,
                      Some(gameInfo),
                      Set(),
                      Some(newPlayerActions.flatMap(_._3).flatten.toList ++ bossCreationActions)
                    )

                  } catch {
                    case e: Throwable =>
                      context.log.error("Failed to follow on EveryoneIsReady", e)
                      throw e
                  }

              }

            case IAmReadyToStart(userId) =>
              context.log.info(s"${userId} is ready to start.")
              val newReadyPlayers = readyPlayers + userId

              if (maybeGameInfo.map(_.players.length).contains(newReadyPlayers.size)) {

                context.log.info(s"Everyone is ready at $now")

                context.self ! MultipleActionsWrapper(maybePreGameActions.get)
                context.self ! GameLoop

                val actionCollector = new GreedyActionGatherer(GameState.empty)
                preGameBehaviour(
                  Nil,
                  actionUpdateCollector,
                  actionCollector,
                  maybeGameInfo.get // get is safe here
                )
              } else {
                setupBehaviour(actionUpdateCollector, maybeGameInfo, newReadyPlayers, maybePreGameActions)
              }
          }

        case _ =>
          context.log.warn("weird")
          Behaviors.unhandled
      }
    }

}
