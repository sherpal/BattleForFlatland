package game.drawers.effects

import gamelogic.physics.Complex
import indigo.*
import gamelogic.entities.WithPosition.Angle

import scala.util.Random

/** A [[Path]] is basically a function from time (Seconds) to a position in the
  * [[gamelogic.physics.Complex]] plane.
  *
  * It has facility methods for creating and composing different paths.
  */
trait Path extends (Seconds => Complex) {

  /** Specifies the duration that this path will last. Is set to None, the duration is considered to
    * be infinite.
    */
  def maybeDuration: Option[Seconds]

  def isFinite: Boolean = maybeDuration.isDefined

  def isOver(time: Seconds): Boolean = maybeDuration.fold(false)(time > _)

  /** Reverse the path. */
  def unary_- : Path = Path.factory(maybeDuration, t => -apply(t))

  /** Translates the path. */
  def +(z: Complex): Path = Path.factory(maybeDuration, t => apply(t) + z)

  /** Apply the conformal map */
  def *(z: Complex): Path = Path.factory(maybeDuration, t => apply(t) * z)

  /** Rotates the path by the given angle. */
  def rotation(angle: Angle): Path = this * Complex.rotation(angle)

  private def randomAngleBetween(min: Angle, max: Angle): Angle =
    Angle.fromDouble(Random.between(min.value, max.value))

  /** Applies a random rotation with angle between `-maxAngle` and `maxAngle` */
  def jitter(maxAngle: Angle): Path = rotation(randomAngleBetween(-maxAngle, maxAngle))

  /** Follows `this` path and then `that` path. If `this` path is infinite, then `that` path is
    * never followed.
    */
  def ++(that: Path): Path =
    maybeDuration match {
      case None => this
      case Some(duration) =>
        val endPosition    = this(duration)
        val thatTranslated = that + endPosition
        val totalDuration  = that.maybeDuration.map(_ + duration)
        Path.factory(
          totalDuration,
          t => if (t < duration) this(t) else thatTranslated(t - duration)
        )
    }

  def stopAfter(time: Seconds): Path =
    Path.factory(Some(maybeDuration.fold(time)(_.min(time))), apply)

}

object Path {

  def factory(duration: Option[Seconds], path: Seconds => Complex): Path = new Path {
    def maybeDuration: Option[Seconds] = duration
    def apply(t: Seconds): Complex     = path(t)
  }

  def infiniteFactory(path: Seconds => Complex): Path = new Path {
    def maybeDuration: Option[Seconds] = None
    def apply(time: Seconds): Complex  = path(time)
  }

  def finiteFactory(duration: Seconds, path: Seconds => Complex): Path = new Path {
    def maybeDuration: Option[Seconds] = Some(duration)
    def apply(time: Seconds): Complex  = path(time)
  }

  val positiveRealLine: Path = Path.infiniteFactory(l => Complex(l.toDouble, 0))

  def positiveSegment(duration: Seconds): Path = positiveRealLine.stopAfter(duration)

  def segment(duration: Seconds, angle: Angle, speed: Double): Path =
    positiveSegment(duration) * Complex.rotation(angle) * speed

  def goUp(duration: Seconds, speed: Double): Path    = segment(duration, Angle.Pi / 2, speed)
  def goDown(duration: Seconds, speed: Double): Path  = -goUp(duration, speed)
  def goRight(duration: Seconds, speed: Double): Path = positiveSegment(duration) * speed
  def goLeft(duration: Seconds, speed: Double): Path  = -goRight(duration, speed)

  def circleLoop(radius: Double, loopDuration: Seconds): Path =
    infiniteFactory(t =>
      radius * Complex.rotation(Angle.Tau * Angle.fromDouble(t.toDouble / loopDuration.toDouble))
    )

  def circle(duration: Seconds, radius: Double): Path =
    circleLoop(radius, duration).stopAfter(duration)

  def arc(duration: Seconds, radius: Double, fromAngle: Angle, toAngle: Angle): Path =
    finiteFactory(
      duration,
      t =>
        radius * Complex.rotation(
          fromAngle + (toAngle - fromAngle) * Angle.fromDouble(t.toDouble / duration.toDouble)
        )
    )

}
