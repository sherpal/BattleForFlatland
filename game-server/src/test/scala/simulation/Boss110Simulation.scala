package simulation

import application.ai.{AIController, GraphManager}
import application.ai.boss.Boss110Controller
import application.ai.boss.boss110units.{
  BigGuyController,
  CreepingShadowController,
  SmallGuyController
}
import application.ai.goodais.bosses.boss110.Boss110Container
import application.masterbehaviours.InGameBehaviour
import gamelogic.abilities.boss.boss110.ExplodeBombs
import gamelogic.abilities.boss.boss110.addsabilities.CreepingShadowTick
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss110.{BigGuy, BombPod, CreepingShadow, SmallGuy}
import gamelogic.entities.boss.dawnoftime.Boss110
import gamelogic.entities.classes.Constants
import gamelogic.gamestate.gameactions.*
import gamelogic.gamestate.gameactions.boss110.{AddBigGuy, AddCreepingShadow, AddSmallGuy}
import gamelogic.gamestate.{ActionGatherer, GameAction, GameState, GreedyActionGatherer}
import gamelogic.utils.{IdGeneratorContainer, IdsProducer}
import models.bff.outofgame.gameconfig.PlayerName

import scala.collection.mutable

/** Plays the Boss110 encounter with the friendly bots, without any client, the same way the game
  * master does. It runs in real time (AIs use the clock), so it is only run on demand:
  *
  * {{{
  * BFF_SIMULATION=240 sbt "game-server/testOnly simulation.Boss110Simulation"
  * }}}
  *
  * where the value is the number of seconds to simulate. Also set `BFF_SIMULATION_TRACE` to print
  * every ability and every damage.
  */
final class Boss110Simulation extends munit.FunSuite with IdsProducer {

  override val munitTimeout = scala.concurrent.duration.Duration(20, "min")

  given IdGeneratorContainer = IdGeneratorContainer.initialIdGeneratorContainer

  def now: Long = System.currentTimeMillis()

  test("The bots play Boss 110") {
    val maybeDuration = sys.env.get("BFF_SIMULATION").flatMap(_.toIntOption)
    assume(maybeDuration.isDefined, "set BFF_SIMULATION to the number of seconds to simulate")
    val durationMillis = maybeDuration.get * 1000L

    var gatherer: ActionGatherer = GreedyActionGatherer(GameState.empty)
    def gameState                = gatherer.currentGameState
    def addActions(actions: Vector[GameAction]): Unit =
      gatherer = gatherer.masterAddAndRemoveActions(actions.sorted.map(_.changeIdWithGen()))._1

    val startTime = now

    // players, as the game master creates them
    val composition = Boss110.maybeAIComposition.get.zipWithIndex
      .groupBy(_._1)
      .toVector
      .flatMap((cls, elems) => elems.indices.map(index => (cls, index)))
    val container = Boss110Container()
    val bots = composition.map { (cls, index) =>
      val playerId = genEntityId()
      addActions(
        AddPlayerByClass(
          genActionId(),
          startTime - 2,
          playerId,
          Boss110.playersStartingPosition,
          cls,
          0xffffff,
          PlayerName.AIPlayerName(cls, index).name
        ) +: cls.builder.startingActions(startTime - 1, playerId)
      )
      playerId -> (container.controller(cls)(index, playerId), s"$cls$index")
    }.toMap

    val staging = Boss110.stagingBossActions(startTime)
    addActions(staging)
    val graphManager =
      GraphManager(
        Vector(
          Constants.playerRadius.toInt,
          math.round(Boss110.shape.radius).toInt,
          math.round(BigGuy.shape.radius).toInt,
          math.round(SmallGuy.shape.radius).toInt
        )
      )
    staging.collect { case action: CreateObstacle =>
      graphManager.addNewObstacle(action, gameState)
    }

    def botActions(): Vector[GameAction] =
      bots.values.toVector.flatMap(_._1.computeActions(gameState, graphManager.graphs))

    // pre-game: bots go to their positions
    while now - startTime < 8000 do {
      addActions(botActions())
      Thread.sleep(5)
    }

    val bossId   = genEntityId()
    val pullTime = now
    val inGame   = InGameBehaviour()
    var pending =
      Vector[GameAction](SpawnBoss(genActionId(), pullTime - 2, bossId, Boss110.name)) ++
        Boss110.initialBossActions(bossId, pullTime - 1) :+ GameStart(genActionId(), pullTime)
    var lastReport = 0L

    val addControllers = mutable.Map.empty[Entity.Id, AIController[?, ?]]

    def seconds(time: Long): String = f"${(time - pullTime) / 1000.0}%6.1fs"
    def name(id: Entity.Id): String =
      bots
        .get(id)
        .fold(
          if id == bossId then "boss"
          else if gameState.allTEntities[BigGuy].contains(id) then "big guy"
          else if gameState.allTEntities[SmallGuy].contains(id) then "small guy"
          else if gameState.allTEntities[CreepingShadow].contains(id) then "shadow"
          else id.toString
        )(_._2)

    var deaths       = Vector.empty[String]
    var shadowTicks  = 0
    var bombsCovered = 0
    var bombsMissed  = 0

    while now - pullTime < durationMillis && !gameState.ended do {
      val stateBefore            = gameState
      val (output, nextGatherer) = inGame.loop(now, pending, gatherer)
      gatherer = nextGatherer
      val time = now

      output.createdActions.foreach {
        case action: AddBigGuy =>
          addControllers(action.entityId) = BigGuyController()
        case action: AddSmallGuy =>
          addControllers(action.entityId) = SmallGuyController()
        case action: AddCreepingShadow =>
          addControllers(action.entityId) = CreepingShadowController()
        // PlaceBombPods also "uses" ExplodeBombs (to put it on cooldown), with no bomb down yet
        case UseAbility(_, _, casterId, _, _: ExplodeBombs)
            if casterId == bossId && stateBefore.allTEntities[BombPod].nonEmpty =>
          val bombs = stateBefore.allTEntities[BombPod].values.toVector
          val covered =
            bombs.filter(ExplodeBombs.isCovered(stateBefore, _, time)).map(_.pos)
          if covered.length == bombs.length then bombsCovered += 1 else bombsMissed += 1
          println(
            s"${seconds(time)} *** bombs explode, ${covered.length}/${bombs.length} covered"
          )
        case UseAbility(_, _, _, _, _: CreepingShadowTick) =>
          shadowTicks += 1
          println(s"${seconds(time)} !!! the shadow touches the boss")
        case UseAbility(_, _, casterId, _, ability)
            if casterId == bossId && ability.castingTime > 0 =>
          println(s"${seconds(time)} boss used ${ability.getClass.getSimpleName}")
        case stun: PutSimpleBuff if stun.resourceIdentifier == gamelogic.buffs.Buff.triangleStun =>
          println(s"${seconds(time)}     ${name(stun.bearerId)} stunned by ${name(stun.sourceId)}")
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
        val boss   = gameState.bosses.get(bossId)
        val shadow = gameState.allTEntities[CreepingShadow].values.headOption
        val shadowGap = (for {
          b <- boss
          s <- shadow
        } yield b.currentPosition(time).distanceTo(s.currentPosition(time)) - s.radius - b.shape.radius)
          .fold("-")(gap => f"$gap%.0f")
        println(
          s"${seconds(time)} boss ${boss.fold("dead")(b => f"${100 * b.life / b.maxLife}%.0f%%")} at ${boss
              .map(_.currentPosition(time))
              .map(p => f"(${p.re}%.0f,${p.im}%.0f)")} | big guys ${gameState
              .allTEntities[BigGuy]
              .size} | small guys ${gameState.allTEntities[SmallGuy].size} | shadow gap $shadowGap | " +
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

      addControllers.filterInPlace((id, _) => gameState.entities.contains(id))

      pending = botActions() ++ Option
        .when(gameState.bosses.contains(bossId))(
          Boss110Controller.computeActions(bossId, gameState, graphManager.graphs)
        )
        .toVector
        .flatten ++ addControllers.toVector.flatMap((id, controller) =>
        controller.computeActions(id, gameState, graphManager.graphs)
      )
      Thread.sleep(5)
    }

    println(
      s"Simulation ended after ${seconds(now)}, deaths: ${deaths.mkString(", ")}, " ++
        s"bombs covered: $bombsCovered, missed: $bombsMissed, shadow ticks: $shadowTicks"
    )
    println(s"Boss life: ${gameState.bosses.get(bossId).map(b => b.life / b.maxLife)}")
    assertEquals(deaths, Vector.empty)
  }

}
