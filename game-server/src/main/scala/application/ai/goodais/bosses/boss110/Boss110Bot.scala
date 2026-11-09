package application.ai.goodais.bosses.boss110

import application.ai.goodais.GoodAIController
import gamelogic.abilities.boss.boss110.PlaceBombPods
import gamelogic.buffs.abilities.classes.TriangleStunDebuff
import gamelogic.entities.Entity
import gamelogic.entities.WithPosition.Angle
import gamelogic.entities.boss.boss110.{BigGuy, BombPod}
import gamelogic.entities.classes.PlayerClass
import gamelogic.gamestate.{GameAction, GameState}
import gamelogic.physics.Complex
import gamelogic.physics.pathfinding.Graph

/** Behaviour shared by all the bots fighting [[gamelogic.entities.boss.dawnoftime.Boss110]].
  *
  * The room is split in two. On the right, a Square tanks the boss and the big guys, two Triangles
  * stun and kill the big guys, and two Pentagons shoot them. On the left, the other Square keeps
  * the small guys away from the boss (so that the creeping shadow stays away from it too), helped
  * by the two other Pentagons. The Hexagons heal from the middle, and the four Pentagons cover the
  * bombs when they are down.
  *
  * Each bot has a place where it wants to be ([[restingPosition]]), and things to do once it is
  * there. Abilities that can be used while moving go in [[instantActions]]; the others (which need
  * the bot to be still) in [[actionsInPlace]].
  */
trait Boss110Bot[EntityType <: PlayerClass] extends GoodAIController[EntityType] {

  /** Where to be, and how close to it is close enough. */
  def restingPosition(gameState: GameState, me: EntityType, time: Long): (Complex, Double)

  /** What to do while moving, or once at the right position. */
  def instantActions(gameState: GameState, me: EntityType, time: Long): Vector[GameAction] =
    Vector.empty

  /** What to do once at the right position, while not moving. */
  def actionsInPlace(gameState: GameState, me: EntityType, time: Long): Vector[GameAction]

  final protected def takeActions(
      gameState: GameState,
      me: EntityType,
      currentPosition: Complex,
      startTime: Long,
      timeSinceLastFrame: Long,
      obstacleGraph: Graph
  ): Vector[GameAction] = {
    val (targetPosition, closeEnough) = restingPosition(gameState, me, startTime)
    val fighting                      = gameState.bosses.nonEmpty

    lazy val instant =
      if fighting then instantActions(gameState, me, startTime) else Vector.empty

    maybeMoveTowards(
      startTime,
      me,
      currentPosition,
      obstacleGraph.nearestLegalPosition(targetPosition),
      obstacleGraph,
      closeEnough
    ) match {
      case None if fighting =>
        if instant.nonEmpty then instant else actionsInPlace(gameState, me, startTime)
      case None => Vector.empty
      case Some(moves) => moves ++ instant
    }
  }

}

object Boss110Bot {

  /** The big guys, from top to bottom. */
  def bigGuys(gameState: GameState): Vector[BigGuy] =
    gameState.allTEntities[BigGuy].values.toVector.sortBy(_.pos.im)

  /** The big guy that everybody on the right kills, while the Triangles keep the others stunned
    * (see [[bigGuyToStun]]).
    */
  def bigGuyToKill(gameState: GameState): Option[BigGuy] = bigGuys(gameState) match {
    case Vector(_, middle, _) => Some(middle)
    case others => others.headOption
  }

  /** The big guy that the Triangle of the given index must keep stunned: the top one for the first,
    * the bottom one for the second (as long as it is not the one to kill).
    */
  def bigGuyToStun(gameState: GameState, triangleIndex: Int): Option[BigGuy] = {
    val toKill = bigGuyToKill(gameState).map(_.id)
    val all    = bigGuys(gameState)
    (if triangleIndex == 0 then all.headOption else all.lastOption)
      .filterNot(bigGuy => toKill.contains(bigGuy.id))
  }

  def isStunnedBy(gameState: GameState, entityId: Entity.Id, stunnerId: Entity.Id): Boolean =
    gameState.allBuffsOfEntity(entityId).exists {
      case stun: TriangleStunDebuff => stun.sourceId == stunnerId
      case _ => false
    }

  /** Where the Pentagon of the given index waits for the bombs, around the area where they fall. */
  def bombWaitingPosition(pentagonIndex: Int): Complex =
    PlaceBombPods.bombsAreaCenter + Complex.polar(
      PlaceBombPods.bombsAreaRadius / 2,
      Angle.fromDouble(bombAngle(pentagonIndex))
    )

  /** The bomb that the Pentagon of the given index must cover, if any. Bombs are sorted by their
    * angle around the bomb area, starting where the first Pentagon waits, so that each Pentagon
    * takes one in its corner.
    */
  def assignedBomb(gameState: GameState, pentagonIndex: Int): Option[BombPod] =
    gameState
      .allTEntities[BombPod]
      .values
      .toVector
      .sortBy { bomb =>
        val angle = (bomb.pos - PlaceBombPods.bombsAreaCenter).arg.value - bombAngle(0)
        (angle % (2 * math.Pi) + 2 * math.Pi) % (2 * math.Pi)
      }
      .lift(pentagonIndex)

  private def bombAngle(pentagonIndex: Int): Double = pentagonIndex * math.Pi / 2 + math.Pi / 4

}
