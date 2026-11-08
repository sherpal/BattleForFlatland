package simulation

import application.ai.{AIController, GraphManager}
import application.ai.boss.Boss102Controller
import application.ai.boss.boss102units.BossHoundController
import application.ai.goodais.bosses.boss102.Boss102Container
import application.masterbehaviours.InGameBehaviour
import gamelogic.entities.Entity
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.boss.boss102.BossHound
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.boss102.AddBossHound
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import models.bff.outofgame.gameconfig.PlayerName

import scala.collection.mutable

/** Plays the Boss102 encounter with the friendly bots, without any client, the same way the game
  * master does. It runs in real time (AIs use the clock), so it is only run on demand:
  *
  * {{{
  * BFF_SIMULATION=180 sbt "game-server/testOnly simulation.Boss102Simulation"
  * }}}
  *
  * where the value is the number of seconds to simulate. Also set `BFF_SIMULATION_TRACE` to print
  * every ability and every damage.
  */
final class Boss102Simulation extends munit.FunSuite with IdsProducer {

  override val munitTimeout = scala.concurrent.duration.Duration(20, "min")

  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def now: Long = System.currentTimeMillis()

  test("The bots play Boss 102") {
    val maybeDuration = sys.env.get("BFF_SIMULATION").flatMap(_.toIntOption)
    assume(maybeDuration.isDefined, "set BFF_SIMULATION to the number of seconds to simulate")
    val durationMillis = maybeDuration.get * 1000L

    var gatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)
    def gameState                = gatherer.currentGameState
    def addActions(actions: Vector[GameAction]): Unit =
      gatherer = gatherer.masterAddAndRemoveActions(actions.sorted.map(_.changeIdWithGen()))._1

    val startTime = now

    // players, as the game master creates them
    val composition = Boss102.maybeAIComposition.get.zipWithIndex
      .groupBy(_._1)
      .toVector
      .flatMap((cls, elems) => elems.indices.map(index => (cls, index)))
    val container = Boss102Container()
    val bots = composition.map { (cls, index) =>
      val playerId = genEntityId()
      addActions(
        AddPlayerByClass(
          genActionId(),
          startTime - 2,
          playerId,
          Boss102.playersStartingPosition,
          cls,
          0xffffff,
          PlayerName.AIPlayerName(cls, index).name
        ) +: cls.builder.startingActions(startTime - 1, playerId)
      )
      playerId -> (container.controller(cls)(index, playerId), s"$cls$index")
    }.toMap

    val staging = Boss102.stagingBossActions(startTime)
    addActions(staging)
    val graphManager =
      GraphManager(Vector(Constants.playerRadius.toInt, Constants.bossRadius.toInt))
    staging.collect { case action: CreateObstacle =>
      graphManager.addNewObstacle(action, gameState)
    }

    def botActions(): Vector[GameAction] =
      bots.values.toVector.flatMap(_._1.computeActions(gameState, graphManager.graphs))

    // pre-game: bots go to their positions
    while now - startTime < 3000 do {
      addActions(botActions())
      Thread.sleep(5)
    }

    val bossId   = genEntityId()
    val pullTime = now
    val inGame   = InGameBehaviour()
    var pending =
      Vector[GameAction](SpawnBoss(genActionId(), pullTime - 2, bossId, Boss102.name)) ++
        Boss102.initialBossActions(bossId, pullTime - 1) :+ GameStart(genActionId(), pullTime)
    var lastReport = 0L

    val houndControllers = mutable.Map.empty[Entity.Id, AIController[?, ?]]

    def seconds(time: Long): String = f"${(time - pullTime) / 1000.0}%6.1fs"
    def name(id: Entity.Id): String =
      bots
        .get(id)
        .fold(
          if id == bossId then "boss"
          else if gameState.allTEntities[BossHound].contains(id) then "hound"
          else id.toString
        )(_._2)

    var deaths = Vector.empty[String]

    while now - pullTime < durationMillis && !gameState.ended do {
      val (output, nextGatherer) = inGame.loop(now, pending, gatherer)
      gatherer = nextGatherer
      val time = now

      output.createdActions.foreach {
        case AddBossHound(_, _, entityId, _) =>
          houndControllers(entityId) = BossHoundController()
        case UseAbility(_, _, casterId, _, ability)
            if casterId == bossId && ability.castingTime > 0 =>
          println(s"${seconds(time)} boss used ${ability.getClass.getSimpleName}")
        case damage: EntityTakesDamage if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(
            s"${seconds(time)}       ${name(damage.entityId)} takes ${damage.amount} from ${name(damage.sourceId)}"
          )
        case UseAbility(_, _, casterId, _, ability) if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(s"${seconds(time)}       ${name(casterId)} uses $ability")
        case _ =>
      }

      bots.foreach { (id, bot) =>
        if !gameState.players.contains(id) && !deaths.contains(bot._2) then {
          deaths :+= bot._2
          println(s"${seconds(time)} XXX ${bot._2} died")
        }
      }

      if time - lastReport > 5000 then {
        lastReport = time
        val boss = gameState.bosses.get(bossId)
        println(
          s"${seconds(time)} boss ${boss.fold("dead")(b => f"${100 * b.life / b.maxLife}%.0f%%")} | hounds ${gameState.allTEntities[BossHound].size} | " +
            gameState.players.values.toVector
              .sortBy(p => name(p.id))
              .map(p => f"${name(p.id)} ${p.life}%.0f/${p.maxLife}%.0f")
              .mkString(" | ")
        )
      }

      houndControllers.filterInPlace((id, _) => gameState.entities.contains(id))

      pending = botActions() ++ Option
        .when(gameState.bosses.contains(bossId))(
          Boss102Controller.computeActions(bossId, gameState, graphManager.graphs)
        )
        .toVector
        .flatten ++ houndControllers.toVector.flatMap((id, controller) =>
        controller.computeActions(id, gameState, graphManager.graphs)
      )
      Thread.sleep(5)
    }

    println(s"Simulation ended after ${seconds(now)}, deaths: ${deaths.mkString(", ")}")
    println(s"Boss life: ${gameState.bosses.get(bossId).map(b => b.life / b.maxLife)}")
    assertEquals(deaths, Vector.empty)
  }

}
