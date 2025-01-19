package application.ai.goodais.bosses.boss102

import application.ai.goodais.GoodAIController
import gamelogic.entities.*
import gamelogic.gamestate.GameState
import gamelogic.physics.Complex
import gamelogic.entities.boss.boss102.DamageZone
import gamelogic.buffs.boss.boss102.LivingDamageZone
import gamelogic.physics.shape.Circle
import gamelogic.physics.shape.Polygon
import gamelogic.entities.boss.dawnoftime.Boss102
import gamelogic.entities.classes.Constants
import gamelogic.entities.boss.boss102.BossHound

trait Boss102GoodAIController[EntityType <: MovingBody & WithPosition]
    extends GoodAIController[EntityType] {

  /** Returns a point in the complex plane where it is "safe" to stand.
    *
    * Safe means
    *
    *   - not on a damage zone
    *   - if the entity has the damage zone debuff, far enough from other players
    *
    * returns None in the case where
    *
    *   - such a safe spot does not exist (the game is probably doomed in that case, hopefully we
    *     don't arrive there)
    *   - the entity is dead
    *   - the entity is already at a safe spot
    *
    * @param gameState
    *   current game state
    * @return
    */
  def findSafeSpot(gameState: GameState, currentTime: Long): Option[Complex] =
    getMe(gameState).flatMap { me =>
      val iHaveTheDebuff = gameState.allBuffsOfEntity(me.id).exists {
        case _: LivingDamageZone => true
        case _                   => false
      }

      val currentPosition = me.currentPosition(currentTime)

      val allCentersToAvoid = gameState.allTEntities[DamageZone].values.toVector.map(_.pos) ++ (
        if iHaveTheDebuff then
          gameState.players.values.toVector
            .filterNot(_.id == me.id)
            .map(_.currentPosition(currentTime))
        else Vector.empty[Complex]
      )

      val allDisks = allCentersToAvoid.map(z => CircleWithPos(z))
      if !allDisks.exists(_.contains(currentPosition)) then None
      else
        pointsGrid
          .sortBy(z => (z - currentPosition).modulus2)
          .find(z => !allDisks.exists(_.contains(z)))
    }

  private val dangerRadius =
    math.max(LivingDamageZone.range, DamageZone.radius) + 2 + Constants.playerRadius
  private val dangerRadiusSquared = math.pow(dangerRadius, 2)

  private val boundsSize = Boss102.size - 10 - Constants.playerRadius
  private val bounds = Polygon(
    Vector(
      Complex(boundsSize, boundsSize),
      Complex(-boundsSize, boundsSize),
      Complex(-boundsSize, -boundsSize),
      Complex(boundsSize, -boundsSize)
    )
  )

  private val pointsGrid = (for {
    x <- (-boundsSize.toInt to boundsSize.toInt by Constants.playerRadius.toInt)
    y <- (-boundsSize.toInt to boundsSize.toInt by Constants.playerRadius.toInt)
  } yield Complex(x, y)).toVector

  private case class CircleWithPos(pos: Complex) {
    def contains(point: Complex) = Circle(dangerRadius).contains(point, pos, 0)

    def closestPointTo(z: Complex): Vector[Complex] =
      if (pos - z).modulus2 < 1 then
        (0 until 4).toVector.map(j => pos + dangerRadius * Complex.rotation(j * 2 * math.Pi / 4))
      else Vector(pos + (z - pos).normalized * dangerRadius)

    def intersectionsWith(that: CircleWithPos): Vector[Complex] =
      val distanceBetweenCircles = (this.pos - that.pos).modulus2
      if distanceBetweenCircles > dangerRadiusSquared then Vector.empty
      else if distanceBetweenCircles < 1 then Vector.empty // they are so close, we don't bother
      else {
        val pointInMiddle    = (this.pos + that.pos) / 2
        val distanceToCenter = (pointInMiddle - pos).modulus
        val intersectionDistanceFromPointInMiddle =
          math.sqrt(dangerRadiusSquared - distanceToCenter * distanceToCenter)
        val dir = (this.pos - that.pos).conjugate.normalized
        Vector(
          pointInMiddle + intersectionDistanceFromPointInMiddle * dir,
          pointInMiddle - intersectionDistanceFromPointInMiddle * dir
        )
      }
  }

  protected def medianOfEntities(entities: Vector[MovingBody], time: Long): Complex = {
    val entitiesPositions = entities.map(_.currentPosition(time))
    val entitiesXMedian   = entitiesPositions.map(_.re).sorted.apply(entitiesPositions.length / 2)
    val entitiesYMedian   = entitiesPositions.map(_.im).sorted.apply(entitiesPositions.length / 2)
    Complex(entitiesXMedian, entitiesYMedian)
  }

  protected def houndsNow(gameState: GameState): Vector[BossHound] =
    gameState.allTEntities[BossHound].values.toVector

}
