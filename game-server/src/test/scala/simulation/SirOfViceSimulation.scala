package simulation

import application.ai.GraphManager
import application.ai.boss.SirOfViceController
import application.ai.goodais.bosses.sirofvice.SirOfViceContainer
import application.masterbehaviours.InGameBehaviour
import gamelogic.abilities.boss.sirofvice.CleansingNova
import gamelogic.buffs.boss.sirofvice.{Inflamed, Punished, Purified}
import gamelogic.entities.Entity
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import models.bff.outofgame.gameconfig.PlayerName

/** Plays the Sir of Vice encounter with the friendly bots, without any client, the same way the
  * game master does. It runs in real time (AIs use the clock), so it is only run on demand:
  *
  * {{{
  * BFF_SIMULATION=90 sbt "game-server/testOnly simulation.SirOfViceSimulation"
  * }}}
  *
  * where the value is the number of seconds to simulate. Also set `BFF_SIMULATION_TRACE` to print
  * every ability and every damage.
  */
final class SirOfViceSimulation extends munit.FunSuite with IdsProducer {

  override val munitTimeout = scala.concurrent.duration.Duration(20, "min")

  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def now: Long = System.currentTimeMillis()

  test("The bots play Sir of Vice") {
    val maybeDuration = sys.env.get("BFF_SIMULATION").flatMap(_.toIntOption)
    assume(maybeDuration.isDefined, "set BFF_SIMULATION to the number of seconds to simulate")
    val durationMillis = maybeDuration.get * 1000L

    var gatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)
    def gameState                = gatherer.currentGameState
    def addActions(actions: Vector[GameAction]): Unit =
      gatherer = gatherer.masterAddAndRemoveActions(actions.sorted.map(_.changeIdWithGen()))._1

    val startTime = now

    // players, as the game master creates them
    val composition = SirOfVice.maybeAIComposition.get.zipWithIndex
      .groupBy(_._1)
      .toVector
      .flatMap((cls, elems) => elems.indices.map(index => (cls, index)))
    val container = SirOfViceContainer()
    val bots = composition.map { (cls, index) =>
      val playerId = genEntityId()
      addActions(
        AddPlayerByClass(
          genActionId(),
          startTime - 2,
          playerId,
          SirOfVice.playersStartingPosition,
          cls,
          0xffffff,
          PlayerName.AIPlayerName(cls, index).name
        ) +: cls.builder.startingActions(startTime - 1, playerId)
      )
      playerId -> (container.controller(cls)(index, playerId), s"$cls$index")
    }.toMap

    val staging = SirOfVice.stagingBossActions(startTime)
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
      Vector[GameAction](SpawnBoss(genActionId(), pullTime - 2, bossId, SirOfVice.name)) ++
        SirOfVice.initialBossActions(bossId, pullTime - 1) :+ GameStart(genActionId(), pullTime)
    var lastReport = 0L

    def seconds(time: Long): String = f"${(time - pullTime) / 1000.0}%6.1fs"
    def name(id: Entity.Id): String =
      bots.get(id).fold(if id == bossId then "boss" else id.toString)(_._2)

    val punishedSince = scala.collection.mutable.Map.empty[Entity.Id, Long]
    var deaths        = Vector.empty[String]
    var flames        = Map.empty[gamelogic.buffs.Buff.Id, Inflamed]

    while now - pullTime < durationMillis && !gameState.ended do {
      val (output, nextGatherer) = inGame.loop(now, pending, gatherer)
      gatherer = nextGatherer
      val time = now

      output.createdActions.foreach {
        case chain: sirofvice.PutHolyWrathChain =>
          println(s"${seconds(time)}     Holy Wrath hit ${chain.positions.length - 1} player(s)")
        case damage: EntityTakesDamage if damage.amount >= CleansingNova.damage =>
          println(s"${seconds(time)} !!! ${name(damage.entityId)} was hit by the Cleansing Nova")
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

      // holy flames: how long before being dispelled
      val currentFlames = gameState.allTBuffs[Inflamed].map(buff => buff.buffId -> buff).toMap
      flames.foreach { (buffId, flame) =>
        if !currentFlames.contains(buffId) then {
          val lasted = (time - flame.appearanceTime) / 1000.0
          val how =
            if time - flame.appearanceTime < Inflamed.duration then "dispelled" else "expired"
          println(
            s"${seconds(time)}     Holy Flame on ${name(flame.bearerId)} $how after ${lasted}s"
          )
        }
      }
      flames = currentFlames

      // punished durations
      bots.keys.foreach { id =>
        val punished = gameState.hasBuffOfType[Punished](id)
        (punished, punishedSince.get(id)) match {
          case (true, None) => punishedSince(id) = time
          case (false, Some(since)) =>
            println(
              s"${seconds(time)}     ${name(id)} was punished for ${(time - since) / 1000.0}s"
            )
            punishedSince.remove(id)
          case _ =>
        }
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
          s"${seconds(time)} boss ${boss.fold("dead")(b => f"${100 * b.life / b.maxLife}%.0f%%")} at ${boss
            .map(_.currentPosition(time))} | " +
            gameState.players.values.toVector
              .sortBy(p => name(p.id))
              .map { p =>
                val buffs = Vector(
                  Option.when(gameState.hasBuffOfType[Inflamed](p.id))("inflamed"),
                  Option.when(gameState.hasBuffOfType[Purified](p.id))("purified")
                ).flatten.mkString(",")
                f"${name(p.id)} ${p.life}%.0f/${p.maxLife}%.0f ${p.currentPosition(time)} $buffs"
              }
              .mkString(" | ")
        )
      }

      pending = botActions() ++ Option
        .when(gameState.bosses.contains(bossId))(
          SirOfViceController.computeActions(bossId, gameState, graphManager.graphs)
        )
        .toVector
        .flatten
      Thread.sleep(5)
    }

    println(s"Simulation ended after ${seconds(now)}, deaths: ${deaths.mkString(", ")}")
    println(s"Boss life: ${gameState.bosses.get(bossId).map(b => b.life / b.maxLife)}")
    assertEquals(deaths, Vector.empty)
  }

}
