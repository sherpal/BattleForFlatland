package simulation

import application.ai.GraphManager
import application.ai.boss.SpiritOfNaraController
import application.ai.boss.spiritofnaraunits.WaterElementalController
import application.ai.goodais.bosses.spiritofnara.SpiritOfNaraContainer
import application.masterbehaviours.InGameBehaviour
import gamelogic.abilities.boss.spiritofnara.{ArcaneExplosion, NaraNuke}
import gamelogic.buffs.boss.spiritofnara.{BlizzardTicks, Chained, FlameWreathWatch, SchoolLockout}
import gamelogic.entities.{Entity, Resource}
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.boss.spiritofnara.WaterElemental
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.spiritofnara.{AddWaterElemental, PutSchoolLockout}
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import models.bff.outofgame.gameconfig.PlayerName

/** Plays the Spirit of Nara encounter with the friendly bots, without any client, the same way the
  * game master does. It runs in real time (AIs use the clock), so it is only run on demand:
  *
  * {{{
  * BFF_SIMULATION=240 sbt "game-server/testOnly simulation.SpiritOfNaraSimulation"
  * }}}
  *
  * where the value is the number of seconds to simulate. Also set `BFF_SIMULATION_TRACE` to print
  * every ability and every damage.
  */
final class SpiritOfNaraSimulation extends munit.FunSuite with IdsProducer {

  override val munitTimeout = scala.concurrent.duration.Duration(20, "min")

  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def now: Long = System.currentTimeMillis()

  test("The bots play the Spirit of Nara") {
    val maybeDuration = sys.env.get("BFF_SIMULATION").flatMap(_.toIntOption)
    assume(maybeDuration.isDefined, "set BFF_SIMULATION to the number of seconds to simulate")
    val durationMillis = maybeDuration.get * 1000L

    var gatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)
    def gameState                = gatherer.currentGameState
    def addActions(actions: Vector[GameAction]): Unit =
      gatherer = gatherer.masterAddAndRemoveActions(actions.sorted.map(_.changeIdWithGen()))._1

    val startTime = now

    // players, as the game master creates them
    val composition = SpiritOfNara.maybeAIComposition.get.zipWithIndex
      .groupBy(_._1)
      .toVector
      .flatMap((cls, elems) => elems.indices.map(index => (cls, index)))
    val container = SpiritOfNaraContainer()
    val bots = composition.map { (cls, index) =>
      val playerId = genEntityId()
      addActions(
        AddPlayerByClass(
          genActionId(),
          startTime - 2,
          playerId,
          SpiritOfNara.playersStartingPosition,
          cls,
          0xffffff,
          PlayerName.AIPlayerName(cls, index).name
        ) +: cls.builder.startingActions(startTime - 1, playerId)
      )
      playerId -> (container.controller(cls)(index, playerId), s"$cls$index")
    }.toMap

    val staging = SpiritOfNara.stagingBossActions(startTime)
    addActions(staging)
    val graphManager = GraphManager(
      Vector(
        Constants.playerRadius.toInt,
        Constants.bossRadius.toInt,
        math.round(WaterElemental.shape.radius).toInt
      )
    )
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
      Vector[GameAction](SpawnBoss(genActionId(), pullTime - 2, bossId, SpiritOfNara.name)) ++
        SpiritOfNara.initialBossActions(bossId, pullTime - 1) :+ GameStart(genActionId(), pullTime)
    var lastReport = 0L
    val elementals = scala.collection.mutable.Map.empty[Entity.Id, WaterElementalController]

    def seconds(time: Long): String = f"${(time - pullTime) / 1000.0}%6.1fs"
    def name(id: Entity.Id): String =
      bots.get(id).fold(if id == bossId then "boss" else s"add$id")(_._2)

    var deaths       = Vector.empty[String]
    var cuts         = 0
    var nukes        = 0
    var hadWreath    = false
    var hadBlizzard  = false
    var blizzardHits = 0

    while now - pullTime < durationMillis && !gameState.ended do {
      val (output, nextGatherer) = inGame.loop(now, pending, gatherer)
      gatherer = nextGatherer
      val time = now

      output.createdActions.foreach {
        case action: AddWaterElemental =>
          elementals(action.entityId) = WaterElementalController()
          println(s"${seconds(time)}     a Water Elemental appears at ${action.position}")
        case _: PutSchoolLockout =>
          cuts += 1
        case damage: EntityTakesDamage
            if damage.amount >= ArcaneExplosion.damage && damage.sourceId == bossId =>
          println(s"${seconds(time)} !!! ${name(damage.entityId)} was caught in the explosion")
        case damage: EntityTakesDamage
            if damage.amount == FlameWreathWatch.explosionDamage && damage.sourceId == bossId =>
          println(s"${seconds(time)} !!! ${name(damage.entityId)} burns in the Flame Wreath")
        case damage: EntityTakesDamage
            if damage.amount == BlizzardTicks.damage && damage.sourceId == bossId =>
          blizzardHits += 1
        case UseAbility(_, _, casterId, _, ability) if casterId == bossId =>
          ability match {
            case _: NaraNuke => nukes += 1
            case _ =>
              val mana = gameState.bosses.get(bossId).fold(0.0)(_.resourceAmount.amount)
              println(
                f"${seconds(time)} boss used ${ability.getClass.getSimpleName} (mana $mana%.0f)"
              )
          }
        case damage: EntityTakesDamage if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(
            s"${seconds(time)}       ${name(damage.entityId)} takes ${damage.amount} from ${name(damage.sourceId)}"
          )
        case UseAbility(_, _, casterId, _, ability) if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(s"${seconds(time)}       ${name(casterId)} uses $ability")
        case _ =>
      }

      val wreath = gameState.allTBuffs[FlameWreathWatch].nonEmpty
      if hadWreath && !wreath then println(s"${seconds(time)}     the Flame Wreath is over")
      hadWreath = wreath
      val blizzard = gameState.allTBuffs[BlizzardTicks].nonEmpty
      if hadBlizzard && !blizzard then
        println(s"${seconds(time)}     the Blizzard is over ($blizzardHits hits)")
        blizzardHits = 0
      hadBlizzard = blizzard

      bots.foreach { (id, bot) =>
        if !gameState.players.contains(id) && !deaths.contains(bot._2) then {
          deaths :+= bot._2
          println(s"${seconds(time)} XXX ${bot._2} died")
        }
      }

      if time - lastReport > 5000 then {
        lastReport = time
        val boss  = gameState.bosses.get(bossId)
        val locks = gameState.allTBuffs[SchoolLockout].map(_.school).mkString(",")
        println(
          s"${seconds(time)} boss ${boss.fold("dead")(b =>
            f"${100 * b.life / b.maxLife}%.0f%% mana ${b.resourceAmount.amount}%.0f locks [$locks]"
          )} | nukes $nukes, cut $cuts | " +
            gameState.players.values.toVector
              .sortBy(p => name(p.id))
              .map { p =>
                val chained = if gameState.hasBuffOfType[Chained](p.id) then " chained" else ""
                val mana =
                  if p.resourceType == Resource.Mana then f" ${p.resourceAmount.amount}%.0fmp"
                  else ""
                f"${name(p.id)} ${p.life}%.0f/${p.maxLife}%.0f (${p
                  .currentPosition(time)
                  .re}%.0f,${p.currentPosition(time).im}%.0f)$mana$chained"
              }
              .mkString(" | ")
        )
      }

      elementals.filterInPlace((id, _) => gameState.entities.contains(id))
      pending = botActions() ++ Option
        .when(gameState.bosses.contains(bossId))(
          SpiritOfNaraController.computeActions(bossId, gameState, graphManager.graphs)
        )
        .toVector
        .flatten ++ elementals.toVector.flatMap((id, controller) =>
        controller.computeActions(id, gameState, graphManager.graphs)
      )
      Thread.sleep(5)
    }

    println(s"Simulation ended after ${seconds(now)}, deaths: ${deaths.mkString(", ")}")
    println(s"Boss life: ${gameState.bosses.get(bossId).map(b => b.life / b.maxLife)}")
    println(s"Nukes: $nukes, cut: $cuts")
    assertEquals(deaths, Vector.empty)
  }

}
