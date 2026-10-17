package application.ai.goodais.bosses.sirofvice

import application.ai.goodais.GoodAIController
import gamelogic.abilities.Ability
import gamelogic.abilities.boss.sirofvice.{CleansingNova, HolyWrath, Punishment, SacredGround}
import gamelogic.buffs.boss.sirofvice.{Punished, Purified}
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.firstraid.SirOfVice
import gamelogic.entities.classes.{Constants, PlayerClass}
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

import scala.reflect.Typeable

/** Behaviour shared by all the bots fighting [[SirOfVice]].
  *
  * Each bot has a place where it wants to be, and things to do once it is there. The place depends
  * on what the boss is doing:
  *   - while the boss casts [[CleansingNova]], everybody hides behind the closest pillar;
  *   - while the boss casts [[Punishment]], players who are not melee step into the
  *     [[SacredGround]] aura, so that its next tick frees them;
  *   - while the boss casts [[HolyWrath]], players too close to its target move away from it, so
  *     that the wrath has nobody to jump to;
  *   - otherwise, each bot goes to its [[restingPosition]], which for ranged players is always out
  *     of the aura.
  *
  * Moving interrupts casting, so a bot only acts (see [[actionsInPlace]]) once it has arrived and
  * stopped.
  */
trait SirOfViceBot[EntityType <: PlayerClass] extends GoodAIController[EntityType] {

  /** Melee players live in the aura of the boss, the others stay out of it. */
  def isMelee: Boolean

  /** Where to be when nothing special is happening, and how close to it is close enough.
    *
    * @param maybeBoss
    *   the boss, or [[scala.None]] before the fight starts
    */
  def restingPosition(
      gameState: GameState,
      me: EntityType,
      maybeBoss: Option[SirOfVice],
      time: Long
  ): (Complex, Double)

  /** What to do once at the right position, while not moving. */
  def actionsInPlace(
      gameState: GameState,
      me: EntityType,
      boss: SirOfVice,
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
    // nothing we do would go through anyway
    if gameState.hasBuffOfType[Punished](me.id) then Vector.empty
    else {
      val maybeBoss = gameState.bosses.values.collectFirst { case boss: SirOfVice => boss }
      val (targetPosition, closeEnough) =
        maybeBoss
          .flatMap(boss => maybeSpecialPosition(gameState, me, boss, startTime, obstacleGraph))
          .getOrElse(restingPosition(gameState, me, maybeBoss, startTime))

      maybeMoveTowards(
        startTime,
        me,
        currentPosition,
        obstacleGraph.nearestLegalPosition(targetPosition),
        obstacleGraph,
        closeEnough
      ).getOrElse(maybeBoss.fold(Vector.empty)(actionsInPlace(gameState, me, _, startTime)))
    }

  private def maybeSpecialPosition(
      gameState: GameState,
      me: EntityType,
      boss: SirOfVice,
      time: Long,
      graph: Graph
  ): Option[(Complex, Double)] = {
    val bossPosition = boss.currentPosition(time)
    val myPosition   = me.currentPosition(time)

    if bossIsCasting[CleansingNova](gameState, boss, time) then
      Some(
        SirOfVice.pillarPositions
          .map(SirOfVice.hidingSpotBehind(_, bossPosition))
          .map(graph.nearestLegalPosition)
          .minBy(_.distanceTo(myPosition)) -> 5.0
      )
    else if !isMelee && bossIsCasting[Punishment](gameState, boss, time) then
      Some(
        bossPosition + (myPosition - bossPosition).safeNormalized * (SacredGround.radius - 40) -> 20.0
      )
    else
      for {
        wrathTargetId <- maybeHolyWrathTarget(gameState, boss, time)
        if wrathTargetId != me.id
        wrathTarget <- gameState.players.get(wrathTargetId)
        targetPosition = wrathTarget.currentPosition(time)
        if myPosition.distanceTo(targetPosition) < HolyWrath.jumpRange + 30
      } yield {
        val away = myPosition - targetPosition
        // if we are on top of each other, any direction will do
        val direction =
          if away.modulus2 > 1e-6 then away.normalized
          else (myPosition - bossPosition).orthogonal.safeNormalized
        targetPosition + direction * (HolyWrath.jumpRange + 40) -> 10.0
      }
  }

  /** The target of the [[HolyWrath]] the boss is currently casting, if any. */
  final def maybeHolyWrathTarget(gameState: GameState, boss: SirOfVice, time: Long) =
    gameState.castingEntityInfo
      .get(boss.id)
      .filter(_.isCasting(time, 0))
      .map(_.ability)
      .collect { case wrath: HolyWrath => wrath.targetId }

  final def bossIsCasting[T <: Ability](gameState: GameState, boss: SirOfVice, time: Long)(using
      Typeable[T]
  ): Boolean =
    gameState.castingEntityInfo.get(boss.id).exists { info =>
      info.isCasting(time, 0) && (info.ability match {
        case _: T => true
        case _    => false
      })
    }

  /** Mana abilities are swallowed while [[Purified]], no need to try. */
  final def isSilenced(gameState: GameState, me: EntityType): Boolean =
    gameState.hasBuffOfType[Purified](me.id)

  /** A spot at `distanceFromCentre` from the centre of the room, in the given direction, but pushed
    * away from the boss if its aura would reach it.
    */
  final def rangedSpot(direction: Angle, maybeBoss: Option[SirOfVice], time: Long): Complex = {
    val spot         = Complex.polar(SirOfViceBot.rangedDistanceFromCentre, direction)
    val bossPosition = maybeBoss.fold(SirOfVice.bossStartingPosition)(_.currentPosition(time))
    val safeDistance = SacredGround.radius + 2 * Constants.playerRadius
    if spot.distanceTo(bossPosition) >= safeDistance then spot
    else bossPosition + (spot - bossPosition).safeNormalized * safeDistance
  }

}

object SirOfViceBot {

  /** Ranged players stand between two pillars, close to the walls, out of the aura of a boss
    * standing in the middle of the room.
    */
  inline def rangedDistanceFromCentre: Double = 290.0

  /** Where the tank keeps the boss: a bit to the left of the centre of the room, which is where the
    * players come from.
    */
  val tankPosition: Complex = Complex(-60, 0)

}
