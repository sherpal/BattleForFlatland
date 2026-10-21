package simulation

import application.ai.{AIController, GraphManager}
import application.ai.boss.Boss105Controller
import application.ai.boss.boss105units.ShadeController
import application.ai.goodais.bosses.boss105.Boss105Container
import application.masterbehaviours.InGameBehaviour
import gamelogic.abilities.boss.boss105.{Resonance, Skewer}
import gamelogic.buffs.boss.boss105.{Mourning, VolatileCharge}
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss105.Shade
import gamelogic.entities.boss.dawnoftime.Boss105
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.boss105.{AddShade, PutYearning, ShadeFalls}
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import models.bff.outofgame.gameconfig.PlayerName

import scala.collection.mutable

/** Plays the Boss105 encounter with the friendly bots, without any client, the same way the game
  * master does. It runs in real time (AIs use the clock), so it is only run on demand:
  *
  * {{{
  * BFF_SIMULATION=240 sbt "game-server/testOnly simulation.Boss105Simulation"
  * }}}
  *
  * where the value is the number of seconds to simulate. Also set `BFF_SIMULATION_TRACE` to print
  * every ability and every damage.
  */
final class Boss105Simulation extends munit.FunSuite with IdsProducer {

  override val munitTimeout = scala.concurrent.duration.Duration(20, "min")

  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def now: Long = System.currentTimeMillis()

  test("The bots play Boss 105") {
    val maybeDuration = sys.env.get("BFF_SIMULATION").flatMap(_.toIntOption)
    assume(maybeDuration.isDefined, "set BFF_SIMULATION to the number of seconds to simulate")
    val durationMillis = maybeDuration.get * 1000L

    var gatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)
    def gameState                = gatherer.currentGameState
    def addActions(actions: Vector[GameAction]): Unit =
      gatherer = gatherer.masterAddAndRemoveActions(actions.sorted.map(_.changeIdWithGen()))._1

    val startTime = now

    // players, as the game master creates them
    val composition = Boss105.maybeAIComposition.get.zipWithIndex
      .groupBy(_._1)
      .toVector
      .flatMap((cls, elems) => elems.indices.map(index => (cls, index)))
    val container = Boss105Container()
    val bots = composition.map { (cls, index) =>
      val playerId = genEntityId()
      addActions(
        AddPlayerByClass(
          genActionId(),
          startTime - 2,
          playerId,
          Boss105.playersStartingPosition,
          cls,
          0xffffff,
          PlayerName.AIPlayerName(cls, index).name
        ) +: cls.builder.startingActions(startTime - 1, playerId)
      )
      playerId -> (container.controller(cls)(index, playerId), s"$cls$index")
    }.toMap

    val staging = Boss105.stagingBossActions(startTime)
    addActions(staging)
    val graphManager =
      GraphManager(
        Vector(
          Constants.playerRadius.toInt,
          Constants.bossRadius.toInt,
          math.round(Shade.shape.radius).toInt
        )
      )
    staging.collect { case action: CreateObstacle =>
      graphManager.addNewObstacle(action, gameState)
    }

    def botActions(): Vector[GameAction] =
      bots.values.toVector.flatMap(_._1.computeActions(gameState, graphManager.graphs))

    // pre-game: bots go to their positions
    while now - startTime < 6000 do {
      addActions(botActions())
      Thread.sleep(5)
    }

    val bossId   = genEntityId()
    val pullTime = now
    val inGame   = InGameBehaviour()
    var pending =
      Vector[GameAction](SpawnBoss(genActionId(), pullTime - 2, bossId, Boss105.name)) ++
        Boss105.initialBossActions(bossId, pullTime - 1) :+ GameStart(genActionId(), pullTime)
    var lastReport = 0L

    val shadeControllers = mutable.Map.empty[Entity.Id, AIController[?, ?]]

    def seconds(time: Long): String = f"${(time - pullTime) / 1000.0}%6.1fs"
    def name(id: Entity.Id): String =
      bots
        .get(id)
        .fold(
          if id == bossId then "boss"
          else if gameState.allTEntities[Shade].contains(id) then "shade"
          else id.toString
        )(_._2)

    var lastShadeStunned = false
    var deaths           = Vector.empty[String]
    var charges          = Map.empty[gamelogic.buffs.Buff.Id, VolatileCharge]
    var resonances       = 0

    def twinDistance(time: Long): String = (for {
      boss  <- gameState.bosses.get(bossId)
      shade <- gameState.allTEntities[Shade].values.headOption
    } yield f"${boss.currentPosition(time).distanceTo(shade.currentPosition(time))}%.0f")
      .getOrElse("-")

    while now - pullTime < durationMillis && !gameState.ended do {
      val (output, nextGatherer) = inGame.loop(now, pending, gatherer)
      gatherer = nextGatherer
      val time = now

      output.createdActions.foreach {
        case AddShade(_, _, entityId, position, _, _, _, _) =>
          println(s"${seconds(time)} *** the Shade is there, at $position")
          shadeControllers(entityId) = ShadeController()
        case stun: PutSimpleBuff if stun.resourceIdentifier == gamelogic.buffs.Buff.triangleStun =>
          println(s"${seconds(time)} *** ${name(stun.bearerId)} stunned by ${name(stun.sourceId)}")
        case damage: EntityTakesDamage
            if gameState.allTEntities[Shade].contains(damage.entityId) &&
              gameState.hasBuffOfType[gamelogic.buffs.boss.boss105.Yearning](damage.entityId) =>
          println(
            s"${seconds(time)}       shade takes ${damage.amount} from ${name(damage.sourceId)} while yearning"
          )
        case _: PutYearning =>
          println(
            s"${seconds(time)} *** the Shade rushes to the boss (twins ${twinDistance(time)} apart)"
          )
        case ShadeFalls(_, _, _, _, _, position) =>
          println(s"${seconds(time)} *** the Shade fell, it will come back at $position")
        case UseAbility(_, _, casterId, _, ability) if ability.isInstanceOf[Resonance] =>
          resonances += 1
          println(s"${seconds(time)} !!! RESONANCE (twins ${twinDistance(time)} apart)")
        case UseAbility(_, _, casterId, _, ability: Skewer) =>
          println(s"${seconds(time)} boss fired Skewer through ${name(ability.targetId)}")
        case UseAbility(_, _, casterId, _, ability)
            if casterId == bossId && ability.castingTime > 0 =>
          println(s"${seconds(time)} boss used ${ability.getClass.getSimpleName}")
        case UseAbility(_, _, casterId, _, ability)
            if casterId != bossId && !bots.contains(casterId) && ability.castingTime > 0 =>
          println(s"${seconds(time)} shade used ${ability.getClass.getSimpleName}")
        case damage: EntityTakesDamage
            if damage.sourceId == bossId && damage.amount >= Skewer.damage * 0.8 - 0.1 =>
          println(
            s"${seconds(time)} !!! ${name(damage.entityId)} takes ${damage.amount} from the boss"
          )
        case damage: EntityTakesDamage if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(
            s"${seconds(time)}       ${name(damage.entityId)} takes ${damage.amount} from ${name(damage.sourceId)}"
          )
        case UseAbility(_, _, casterId, _, ability) if sys.env.contains("BFF_SIMULATION_TRACE") =>
          println(s"${seconds(time)}       ${name(casterId)} uses $ability")
        case _ =>
      }

      val shadeStunned = gameState
        .allTEntities[Shade]
        .keys
        .exists(gameState.hasBuffOfType[gamelogic.buffs.abilities.classes.TriangleStunDebuff](_))
      if shadeStunned != lastShadeStunned then {
        println(s"${seconds(time)}   shade stunned: $shadeStunned")
        lastShadeStunned = shadeStunned
      }

      // volatile charges: how long before being dispelled
      val currentCharges = gameState.allTBuffs[VolatileCharge].map(b => b.buffId -> b).toMap
      charges.foreach { (buffId, charge) =>
        if !currentCharges.contains(buffId) then {
          val lasted = (time - charge.appearanceTime) / 1000.0
          val how =
            if time - charge.appearanceTime < VolatileCharge.duration - 100 then "dispelled"
            else "EXPLODED"
          println(s"${seconds(time)}     Charge on ${name(charge.bearerId)} $how after ${lasted}s")
        }
      }
      charges = currentCharges

      bots.foreach { (id, bot) =>
        if !gameState.players.contains(id) && !deaths.contains(bot._2) then {
          deaths :+= bot._2
          println(s"${seconds(time)} XXX ${bot._2} died")
        }
      }

      if time - lastReport > 5000 then {
        lastReport = time
        val boss    = gameState.bosses.get(bossId)
        val shade   = gameState.allTEntities[Shade].values.headOption
        val exposed = gameState.hasBuffOfType[Mourning](bossId)
        println(
          s"${seconds(time)} boss ${boss.fold("dead")(b => f"${100 * b.life / b.maxLife}%.0f%%")}${if exposed then " EXPOSED" else ""} at ${boss
            .map(_.currentPosition(time))
            .map(p => f"(${p.re}%.0f,${p.im}%.0f)")} | shade ${shade
            .fold("-")(s =>
              f"${100 * s.life / s.maxLife}%.0f%% at (${s.currentPosition(time).re}%.0f,${s.currentPosition(time).im}%.0f) -> ${name(s.targetId)}"
            )} | twins ${twinDistance(time)} apart | " +
            gameState.players.values.toVector
              .sortBy(p => name(p.id))
              .map { p =>
                val position = p.currentPosition(time)
                f"${name(p.id)} ${p.life}%.0f/${p.maxLife}%.0f (${position.re}%.0f,${position.im}%.0f)${if p.resourceType == gamelogic.entities.Resource.Mana
                then f" m${p.resourceAmount.amount}%.0f"
                else ""}"
              }
              .mkString(" | ")
        )
      }

      shadeControllers.filterInPlace((id, _) => gameState.entities.contains(id))

      pending = botActions() ++ Option
        .when(gameState.bosses.contains(bossId))(
          Boss105Controller.computeActions(bossId, gameState, graphManager.graphs)
        )
        .toVector
        .flatten ++ shadeControllers.toVector.flatMap((id, controller) =>
        controller.computeActions(id, gameState, graphManager.graphs)
      )
      Thread.sleep(5)
    }

    println(
      s"Simulation ended after ${seconds(now)}, deaths: ${deaths.mkString(", ")}, resonance pulses: $resonances"
    )
    println(s"Boss life: ${gameState.bosses.get(bossId).map(b => b.life / b.maxLife)}")
    assertEquals(deaths, Vector.empty)
  }

}
