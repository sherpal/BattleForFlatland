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
import application.ai.AIManager
import scala.concurrent.ExecutionContext

class GameMaster(
    actionTranslator: ActionTranslator,
    gameInfo: MenuGameWithPlayers,
    playerMap: Map[String, ConnectedPlayerInfo]
)(using
    idGeneratorContainer: IdGeneratorContainer
)(using ExecutionContext) {

  def bossName = gameInfo.game.gameConfiguration.bossName

  @volatile() private var actionGatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)

  val aiManager = AIManager(() => actionGatherer.currentGameState, actionTranslator)

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
      run()
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
                ) +: info.playerClass.builder
                  .startingActions(timeNow - 1, playerId, idGeneratorContainer)
              ),
              info.playerName
            )

          }

          log.info("Creating Boss Creation actions")
          val bossCreationActions = BossFactory.factoriesByBossName
            .get(gameInfo.game.gameConfiguration.bossName)
            .fold(Vector.empty[GameAction])(_.stagingBossActions(timeNow, idGeneratorContainer))

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

  private def run(): Unit = {
    val thread = new Thread(new Runnable {
      def run(): Unit = {
        println("LFG!!!")
        val preGameBehaviour = PreGameBehaviour()

        // looping the pre game behaviour until players start the game
        while (!shouldBegin.get)
          preGameBehaviour.completeLoop()

        // compute initial actions (spawning boss with its initial actions and other related stuff)
        val timeNow = now
        val bossCreationActions = {
          val bossId = idGeneratorContainer.entityIdGenerator()
          SpawnBoss(0L, timeNow - 2, bossId, bossName) +: BossFactory.factoriesByBossName
            .get(bossName)
            .fold(Vector.empty[GameAction])(
              _.initialBossActions(bossId, timeNow - 1, idGeneratorContainer)
            )
            .toVector
        }
        val bossFactories = BossFactory.factoriesByBossName.get(bossName).toVector

        val newPendingActions = bossCreationActions :+ GameStart(0L, now)
        actionBuffer.addActions(newPendingActions)

        val inGameBehaviour = InGameBehaviour()

        // looping the in game behaviour until the game stops
        while (!actionGatherer.currentGameState.ended)
          inGameBehaviour.completeLoop()

        // applying and of game actions
        val endOfGameActions =
          BossFactory
            .factoriesByBossName(gameInfo.game.gameConfiguration.bossName)
            .whenBossDiesActions(actionGatherer.currentGameState, now, idGeneratorContainer)

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
    private def completeLoop(): Unit = {
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
      Thread.sleep(3)
    }

  }

  start()

}
