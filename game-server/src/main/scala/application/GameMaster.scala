package application

import models.bff.outofgame.MenuGameWithPlayers
import gamelogic.utils.IdGeneratorContainer
import gamelogic.entities.boss.BossFactory
import models.bff.outofgame.gameconfig.PlayerName
import gamelogic.physics.Complex
import gamelogic.gamestate.GameAction
import models.bff.ingame.InGameWSProtocol
import models.bff.outofgame.gameconfig.PlayerName.HumanPlayerName
import gamelogic.gamestate.gameactions.AddPlayerByClass
import application.masterbehaviours.GameMasterBehaviour
import java.util.concurrent.atomic.AtomicBoolean
import application.masterbehaviours.*
import gamelogic.gamestate.ActionGatherer
import gamelogic.gamestate.GameState
import gamelogic.gamestate.GreedyActionGatherer
import gamelogic.gamestate.gameactions.{GameStart, SpawnBoss}
import gamelogic.gamestate.AddAndRemoveActions
import application.ai.{AIManager, GoodAIManager}
import scala.concurrent.ExecutionContext
import gamelogic.entities.Entity
import gamelogic.utils.IdsProducer
import gamelogic.docs.BossMetadata

class GameMaster(
    actionTranslator: ActionTranslator,
    gameInfo: MenuGameWithPlayers,
    playerMap: Map[String, ConnectedPlayerInfo]
)(using
    idGeneratorContainer: IdGeneratorContainer
)(using ExecutionContext)
    extends IdsProducer
    with TimeManager {

  def bossName = gameInfo.game.gameConfiguration.bossName

  @volatile() private var actionGatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)

  val aiManager = AIManager(() => actionGatherer.currentGameState, actionTranslator)
  val goodAiManager =
    BossMetadata.maybeMetadataByName(gameInfo.game.gameConfiguration.bossName) match {
      case Some(bossMetadata) =>
        Some(GoodAIManager(bossMetadata, () => actionGatherer.currentGameState, actionTranslator))
      case None =>
        println(s"Woopsies, No metadata for ${gameInfo.game.gameConfiguration.bossName}")
        None
    }

  /** Set of players that have received the boss starting position and their entity ids, and have
    * done everything they have to before the game starts (loading assets, creating frontend
    * stuff...)
    *
    * Once this set is full, the "pre game" mode enters and players can start moving and preparing
    * for the game like putting markers and stuff like that.
    *
    * Once they hit the "lets begin" button, the boss will start moving.
    */
  private var readyPlayers = Set.empty[String]

  private var currentBehaviour: GameMasterBehaviour = scala.compiletime.uninitialized

  def now = System.currentTimeMillis

  inline def actionBuffer = actionTranslator.actionBuffer

  val log = Logger()

  def newPlayerIsReady(userName: String): Unit = synchronized {
    readyPlayers += userName
    if readyPlayers.size == gameInfo.players.length then {
      // every one is ready, we can enter "pre game" mode
      aiManager.start()
      goodAiManager.foreach(_.start())
      run()
      log.info("Warning all players that everyone is ready")
      playerMap.values.foreach(_.send(InGameWSProtocol.EveryoneIsReady))
    }
  }

  private def start(): Unit = {
    log.info("Everyone is ready")
    // first message that kick off the actor and will trigger others
    val timeNow = now

    gameInfo.game.gameConfiguration.asValid match {
      case None =>
        val message = """I received game information with an invalid game configuration.
                      |This should not happen. Something went terribly wrong upstream.
                      |
                      |Shutting down now...
                      |""".stripMargin
        log.error(message)
        throw IllegalStateException(message)
      case Some(gameConfiguration) =>
        try {

          def maybeBossFactory =
            BossFactory.factoriesByBossName.get(gameConfiguration.bossName).toVector

          if (maybeBossFactory.isEmpty) {
            log.warn("Starting Game without boss, that's weird.")
          }

          log.info("Creating refs by name map.")
          val refsByName: Map[PlayerName, ConnectedPlayerInfo] =
            gameInfo.players
              .map(user => HumanPlayerName(user.name) -> playerMap(user.name))
              .toMap

          log.info("Computing player positions.")
          val playersPosition = maybeBossFactory
            .map(_.playersStartingPosition)
            .head

          val bossStartingPosition =
            maybeBossFactory.headOption.fold(Complex.zero)(_.bossStartingPosition)

          log.info("Creating NewPlayer actions")
          val newPlayerActions = gameConfiguration.playersInfo.values.map { info =>
            val playerId = genEntityId()
            (
              refsByName.get(info.playerName),
              if (info.playerType.playing) playerId else Entity.Id.dummy,
              Option.when(info.playerType.playing)(
                AddPlayerByClass(
                  idGeneratorContainer.actionId(),
                  timeNow - 2,
                  playerId,
                  playersPosition,
                  info.playerClass,
                  info.playerColour.intColour,
                  info.playerName.name
                ) +: info.playerClass.builder
                  .startingActions(timeNow - 1, playerId)
              ),
              info.playerName
            )

          }

          log.info("Creating Boss Creation actions")
          val bossCreationActions = BossFactory.factoriesByBossName
            .get(gameInfo.game.gameConfiguration.bossName)
            .fold(Vector.empty[GameAction])(_.stagingBossActions(timeNow))

          log.info(s"Boss creation action at $timeNow")

          newPlayerActions.foreach {
            case (Some(ref), playerId, _, _) =>
              ref.send(InGameWSProtocol.YourEntityIdIs(playerId))
              ref.send(
                InGameWSProtocol.StartingBossPosition(
                  bossStartingPosition.re,
                  bossStartingPosition.im
                )
              )
            case (None, playerId, _, name) =>
          }

          val allGeneratedActions =
            newPlayerActions.flatMap(_._3).flatten.toVector ++ bossCreationActions
          println(s"${allGeneratedActions.length} generated actions.")

          actionBuffer.addActions(allGeneratedActions)
        } catch {
          case e: Throwable =>
            log.error("Failed to follow on EveryoneIsReady", e)
            throw e
        }

    }
  }

  private val shouldBegin = AtomicBoolean(false)

  def letsBegin(): Unit = shouldBegin.set(true)

  private var runningThread: Option[Thread] = Option.empty

  private val performanceMonitor = PerformanceMonitor()

  def performanceSummary = performanceMonitor.retrieveSummary

  private def run(): Unit = {
    val thread = new Thread(new Runnable {
      def run(): Unit = {
        var lastLoopTime = 0L

        println("LFG!!!")
        val preGameBehaviour = PreGameBehaviour()

        // looping the pre game behaviour until players start the game
        lastLoopTime = now
        while (!shouldBegin.get)
          preGameBehaviour.completeLoop(lastLoopTime)
          val newLoopTime = now
          performanceMonitor.addInfo(newLoopTime - lastLoopTime)
          lastLoopTime = newLoopTime

        println("Woot, game begins!!!")

        // compute initial actions (spawning boss with its initial actions and other related stuff)
        val timeNow = now
        val bossCreationActions = {
          val bossId = genEntityId()
          SpawnBoss(
            idGeneratorContainer.actionId(),
            timeNow - 2,
            bossId,
            bossName
          ) +: BossFactory.factoriesByBossName
            .get(bossName)
            .fold(Vector.empty[GameAction])(
              _.initialBossActions(bossId, timeNow - 1)
            )
            .toVector
        }
        val bossFactories = BossFactory.factoriesByBossName.get(bossName).toVector

        val newPendingActions =
          bossCreationActions :+ GameStart(idGeneratorContainer.actionId(), now)
        actionBuffer.addActions(newPendingActions)

        val inGameBehaviour = InGameBehaviour()

        // looping the in game behaviour until the game stops
        lastLoopTime = now
        while (!actionGatherer.currentGameState.ended)
          inGameBehaviour.completeLoop(lastLoopTime)
          val newLoopTime = now
          performanceMonitor.addInfo(newLoopTime - lastLoopTime)
          lastLoopTime = newLoopTime

        // applying and of game actions
        val endOfGameActions =
          BossFactory
            .factoriesByBossName(gameInfo.game.gameConfiguration.bossName)
            .whenBossDiesActions(actionGatherer.currentGameState, now)

        val (endOfGameCollector, oldestTimeToRemove, idsToRemove) =
          actionGatherer.masterAddAndRemoveActions(endOfGameActions)
        actionGatherer = endOfGameCollector
        actionTranslator.dispatchGameActions(
          AddAndRemoveActions(
            endOfGameActions,
            oldestTimeToRemove,
            idsToRemove
          )
        )

        // maybe here we should loop again

        println("Game has ended.")
      }
    })
    thread.start()
    runningThread = Some(thread)
  }

  extension (behaviour: GameMasterBehaviour) {
    private def completeLoop(startTime: Long): Unit = {
      val (output, newGatherer) = behaviour.loop(now, actionBuffer.flush(), actionGatherer)
      actionGatherer = newGatherer
      if output.createdActions.nonEmpty || output.idsOfIdsToRemove.nonEmpty then {
        actionTranslator.dispatchGameActions(
          AddAndRemoveActions(
            output.createdActions,
            output.oldestTimeToRemove,
            output.idsOfIdsToRemove
          )
        )
      }
      val endTime = now
      sleep(8 - (endTime - startTime))
    }

  }

  start()
}
