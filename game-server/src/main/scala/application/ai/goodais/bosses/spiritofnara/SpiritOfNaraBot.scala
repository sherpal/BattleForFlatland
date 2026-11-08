package application.ai.goodais.bosses.spiritofnara

import application.ai.goodais.GoodAIController
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.spiritofnara.{ArcaneExplosion, FlameWreath}
import gamelogic.buffs.boss.spiritofnara.{Chained, Polymorphed, Pulled}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SpiritOfNara
import gamelogic.entities.boss.spiritofnara.{BlizzardCloud, FlameWreathRing}
import gamelogic.entities.classes.{Constants, PlayerClass}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.reflect.Typeable

/** Behaviour shared by all the bots fighting the [[SpiritOfNara]].
  *
  * Each bot has a spot around the boss (melee players right next to it, the others out of reach of
  * its Counterspell), and things to do once there. The spot depends on what the boss is doing:
  *   - after the Magnetic Pull and during the [[ArcaneExplosion]], everybody runs straight away
  *     from the boss, out of the explosion;
  *   - while the [[FlameWreath]] is cast and its rings burn, nobody moves;
  *   - while the Blizzard goes around, ranged players move along their circle to stay out of it;
  *   - while polymorphed, pulled or chained, there is nothing to do but wait (or cast).
  *
  * Moving interrupts casting, so a bot only acts (see [[actionsInPlace]]) once it has arrived and
  * stopped.
  */
trait SpiritOfNaraBot[EntityType <: PlayerClass] extends GoodAIController[EntityType] {

  /** Where the bot stands, seen from the boss. */
  def homeAngle: Angle

  /** How far from the boss the bot stands. */
  def homeDistance: Double

  /** What to do once at the right position, while not moving. */
  def actionsInPlace(
      gameState: GameState,
      me: EntityType,
      boss: SpiritOfNara,
      time: Long
  ): Vector[GameAction]

  final protected def takeActions(
      gameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] =
    if gameState.hasBuffOfType[Polymorphed](me.id) || gameState.hasBuffOfType[Pulled](me.id) then
      Vector.empty
    else {
      val maybeBoss = gameState.bosses.values.collectFirst { case boss: SpiritOfNara => boss }
      val mustStandStill = gameState.hasBuffOfType[Chained](me.id) ||
        gameState.allTEntities[FlameWreathRing].nonEmpty ||
        maybeBoss.exists(bossIsCasting[FlameWreath](gameState, _, startTime))

      val maybeMove =
        if mustStandStill then
          stopMoving(startTime, me, currentPosition, me.rotation).map(Vector(_))
        else {
          val (targetPosition, closeEnough) =
            maybeBoss.fold(SpiritOfNara.playersStartingPosition -> 10.0)(
              wantedPosition(gameState, me, _, startTime)
            )
          maybeMoveTowards(
            startTime,
            me,
            currentPosition,
            obstacleGraph.nearestLegalPosition(targetPosition),
            obstacleGraph,
            closeEnough
          )
        }

      maybeMove.getOrElse(maybeBoss.fold(Vector.empty)(actionsInPlace(gameState, me, _, startTime)))
    }

  private def wantedPosition(
      gameState: GameState,
      me: EntityType,
      boss: SpiritOfNara,
      time: Long
  ): (Complex, Double) = {
    val bossPosition = boss.currentPosition(time)
    val myPosition   = me.currentPosition(time)

    if boss.arcaneExplosionPending || bossIsCasting[ArcaneExplosion](gameState, boss, time) then {
      val away = (myPosition - bossPosition).safeNormalized
      val direction =
        if away.modulus2 > 0.5 then away else Complex.rotation(homeAngle)
      (bossPosition + direction * (ArcaneExplosion.radius + 40)) -> 15.0
    } else {
      val clouds = gameState.allTEntities[BlizzardCloud].values
      def isSafe(spot: Complex): Boolean = clouds.forall(cloud =>
        SpiritOfNaraBot.lookAhead.forall(delay =>
          spot.distanceTo(cloud.centreAt(time + delay)) >
            BlizzardCloud.radius + Constants.playerRadius + 15
        )
      )
      // the closest spot to home that the storm won't reach soon
      val spot = SpiritOfNaraBot.angleOffsets.iterator
        .map(offset => bossPosition + Complex.polar(homeDistance, homeAngle + offset))
        .find(isSafe)
        .getOrElse(bossPosition + Complex.polar(homeDistance, homeAngle))
      spot -> 10.0
    }
  }

  final def bossIsCasting[T <: Ability](gameState: GameState, boss: SpiritOfNara, time: Long)(using
      Typeable[T]
  ): Boolean =
    gameState.castingEntityInfo.get(boss.id).exists { info =>
      info.isCasting(time, 0) && (info.ability match {
        case _: T => true
        case _ => false
      })
    }

}

object SpiritOfNaraBot {

  /** Out of reach of the Counterspell, but well within the Arcane Explosion. */
  inline def rangedDistance: Double = 185.0

  /** Melee players stand right next to the boss. */
  inline def meleeDistance: Double = Constants.bossRadius + Constants.playerRadius + 8

  /** When the storm will be checked, from now (in ms). */
  val lookAhead: Vector[Long] = Vector(0L, 1500L, 3000L, 4500L)

  /** The spots tried when dodging the storm, from home and further and further away from it. */
  val angleOffsets: Vector[Angle] =
    Vector(0.0, 0.4, -0.4, 0.8, -0.8, 1.2, -1.2, 1.6, -1.6, 2.0, -2.0, 2.4, -2.4, 2.8, -2.8)
      .map(Angle.fromDouble)

}
