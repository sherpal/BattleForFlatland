package application.ai.goodais.bosses.boss104

import application.ai.goodais.classes.HexagonAIController
import application.ai.utils.maybeAbilityUsage
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.boss104.DeathMarkTriangleAttack
import gamelogic.abilities.hexagon.CreateHexagonZone
import gamelogic.buffs.Buff
import gamelogic.entities.Entity
import gamelogic.entities.boss.boss104.DeathMarkTriangle
import gamelogic.entities.boss.dawnoftime.Boss104
import gamelogic.entities.classes.Hexagon
import gamelogic.entities.classes.hexagon.HexagonZone
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph
import utils.misc.RGBColour

final case class HexagonForBoss104(index: Int, entityId: Entity.Id)
    extends HexagonAIController(index)
    with DeathMarkTriangleAware[Hexagon] {

  protected def takeActions(
      gameState: GameState,
      me: Hexagon,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = maybeHandleDeathMarkTriangleWithZone(gameState, me, startTime).getOrElse {
    import gamelogic.physics.Complex.DoubleWithI
    val (previousPosition, currentPosition, travelledDistance) = someDistanceInfo(startTime, me)

    val actions: Vector[GameAction] = gameState.bosses.values.headOption match {
      case Some(theBoss) =>
        val maybeTank = maybeTankWithNotEnoughHot(gameState, me)

        val entityWithBossDebuffAndNoHotFromMe = gameState.players.values.find { player =>
          val buffs = gameState.allBuffsOfEntity(player.id)
          buffs.count(_.resourceIdentifier == Buff.boss101BigDotIdentifier) > countOfMyHotOnEntity(
            gameState,
            player.id,
            me
          )
        }

        val putHot =
          putHotOnFirstWithThreshold(
            gameState,
            me,
            startTime,
            0.5
          )

        val maybeFlashHeal = maybeFlashHealWithThreshold(gameState, startTime, me, 0.3)

        putHot.orElse(maybeFlashHeal).toVector

      case None =>
        val targetPosition = Boss104.bossStartingPosition - 200.i + (index - 0.5) * 60
        preGameMovement(startTime, me, currentPosition, targetPosition, travelledDistance)
    }

    actions

  }

  /** The usual [[DeathMarkTriangleAware]] behaviour, but taking the time to put down a zone. */
  private def maybeHandleDeathMarkTriangleWithZone(
      gameState: GameState,
      me: Hexagon,
      time: Long
  ): Option[Vector[GameAction]] =
    maybeZoneForDeathMarkTriangle(gameState, me, time)
      .orElse(maybeHandleDeathMarkTriangle(gameState, me, time))

  /** During the second stage of the [[DeathMarkTriangle]] attack, everybody is packed in the
    * triangle: the perfect moment for a [[HexagonZone]] at its centre.
    *
    * We don't wait for the second stage to begin: the cast starts [[zoneAnticipation]] before the
    * first stage resolves (we know when by looking at the boss' cast), so that the zone is up as
    * early as possible. Since it takes [[CreateHexagonZone.castingTime]] to cast, it ends shortly
    * after the second stage begins, and the zone covers almost all of it.
    *
    * Moving interrupts a cast, so:
    *   - while casting the zone, we do nothing at all;
    *   - during the first stage, we only start casting once arrived at our safe spot (stopping
    *     before would be deadly);
    *   - during the second stage, we stop moving to cast (the zone is the priority), and run to the
    *     centre afterwards.
    *
    * Returns [[scala.None]] when there is nothing to do with the zone, so that the usual death mark
    * triangle behaviour takes over.
    */
  private def maybeZoneForDeathMarkTriangle(
      gameState: GameState,
      me: Hexagon,
      time: Long
  ): Option[Vector[GameAction]] =
    gameState.allTEntities[DeathMarkTriangle].values.headOption.flatMap { triangle =>
      val isCastingZone = gameState.castingEntityInfo
        .get(me.id)
        .exists(info => info.ability.isInstanceOf[CreateHexagonZone] && info.isCasting(time, 0))

      lazy val firstStageAboutToResolve = gameState.castingEntityInfo.valuesIterator.exists(info =>
        info.ability.isInstanceOf[DeathMarkTriangleAttack] &&
          info.remainingCastingTime(time, 0) <= zoneAnticipation
      )

      lazy val shouldCastZone =
        if triangle.isLastStage then true
        else !me.moving && firstStageAboutToResolve

      if isCastingZone then Some(Vector.empty)
      else if !shouldCastZone then None
      else
        maybeAbilityUsage(
          me,
          CreateHexagonZone(
            Ability.UseId.dummy,
            time,
            me.id,
            triangle.pos,
            HexagonZone.healOnTick,
            0,
            RGBColour.fromIntColour(me.colour).withAlpha(0.5)
          ),
          gameState
        ).startCasting.map { cast =>
          // stopping and starting the cast in the same loop could record the cast's starting
          // position before the stop, and get it interrupted: we cast on the next loop instead
          if me.moving then stopMoving(time, me, me.currentPosition(time), me.rotation).toVector
          else Vector(cast)
        }
    }

  /** How long before the first stage resolves we start casting the zone. */
  private inline def zoneAnticipation: Long = 1000L

}
