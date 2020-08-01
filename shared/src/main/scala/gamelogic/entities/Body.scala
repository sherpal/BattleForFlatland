package gamelogic.entities

import gamelogic.entities.WithPosition.Angle
import gamelogic.physics.Complex
import gamelogic.physics.shape.Shape

/**
  * A Body has a [[gamelogic.physics.shape.Shape]] attached to it and can collide other Bodies.
  */
trait Body extends WithPosition {
  def shape: Shape

  def rotation: Angle

  /**
    * Checks if this MovingBody collides with that Moving body.
    * @param that        other body
    * @param currentTime current time to check collision
    */
  def collides(that: Body, currentTime: Long): Boolean =
    shape.collides(currentPosition(currentTime), rotation, that.shape, that.currentPosition(currentTime), that.rotation)

  /**
    * Checks whether this Body collides the given shape, translated and rotated.
    */
  def collidesShape(thatShape: Shape, thatTranslation: Complex, thatRotation: Double, currentTime: Long): Boolean =
    shape.collides(currentPosition(currentTime), rotation, thatShape, thatTranslation, thatRotation)

  /** Checks whether this [[Body]] contains the given point. */
  def containsPoint(point: Complex, currentTime: Long): Boolean =
    shape.contains(point, currentPosition(currentTime), rotation)

  /**
    * Finds the first position, going from pos and towards rotation, where this body does not collide the bodies.
    *
    * @param rotation  The angle towards which we need to go.
    * @param bodies    All the bodies we want to avoid.
    * @param precision Steps by which trying.
    * @return          First position where this body will have no collision.
    */
  def firstValidPosition(rotation: Angle, bodies: Iterable[Body], precision: Int = 5): Complex = {
    val dir = Complex.rotation(rotation) * precision

    @scala.annotation.tailrec
    def tryNextPos(nextPos: Complex): Complex =
      if (!bodies.exists(body => body.shape.collides(body.pos, body.rotation, this.shape, nextPos, this.rotation)))
        nextPos
      else
        tryNextPos(nextPos + dir)

    tryNextPos(pos + dir)
  }

  /**
    * Find the position z closest to targetPosition, on the segment [Complex(xPos, yPos), targetPosition] such that
    * there is no collision on the segment.
    *
    * @param targetPosition The position that we try to reach
    * @param bodies         All the Bodies that can enter in collision
    * @param precision      The step moving forward (typically 5-10 pixels)
    * @return               The final position
    */
  def lastValidPosition(targetPosition: Complex, bodies: Iterable[Body], precision: Int = 5): Complex = {
    val direction = targetPosition - pos
    val dist      = direction.modulus

    if (dist < 2)
      pos
    else {
      val dir = precision * direction / dist

      @scala.annotation.tailrec
      def tryNextPos(curPos: Complex): Complex = {
        val nextPos = curPos + dir

        if (bodies.exists(
              body => body.shape.collides(body.pos, body.rotation, shape, nextPos, rotation)
            )) // if the next position collides, then we should stop here
          curPos
        else if ((nextPos - pos).modulus > dist)
          targetPosition
        else // otherwise, we go further
          tryNextPos(nextPos)

      }

      tryNextPos(pos)
    }
  }

}
