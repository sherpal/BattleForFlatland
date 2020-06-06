package game.ui.effects

import gamelogic.physics.Complex

/**
  * A [[Path]] is basically a function from time (Long) to a position in the [[gamelogic.physics.Complex]] plane.
  *
  * It has facility methods for creating and composing different paths.
  */
trait Path extends (Long => Complex) {

  /**
    * Specifies the duration that this path will last.
    * Is set to None, the duration is considered to be infinite.
    */
  def maybeDuration: Option[Long]

  def isFinite: Boolean = maybeDuration.isDefined

  def isOver(time: Long): Boolean = maybeDuration.fold(false)(time > _)

  /** Reverse the path. */
  def unary_- : Path = Path.factory(maybeDuration, t => -apply(t))

  /** Translates the path. */
  def +(z: Complex): Path = Path.factory(maybeDuration, t => apply(t) + z)

  /** Apply the conformal map */
  def *(z: Complex): Path = Path.factory(maybeDuration, t => apply(t) * z)

  /**
    * Follows `this` path and then `that` path. If `this` path is infinite, then `that` path is never followed.
    */
  def ++(that: Path): Path =
    maybeDuration match {
      case None => this
      case Some(duration) =>
        val endPosition    = this(duration)
        val thatTranslated = that + endPosition
        val totalDuration  = that.maybeDuration.map(_ + duration)
        Path.factory(totalDuration, t => if (t < duration) this(t) else thatTranslated(t - duration))
    }

  def stopAfter(time: Long): Path = Path.factory(Some(maybeDuration.fold(time)(_ min time)), apply)

}

object Path {

  def factory(duration: Option[Long], path: Long => Complex): Path = {
    val _duration = duration
    new Path {
      def maybeDuration: Option[Long] = _duration

      def apply(t: Long): Complex = path(t)
    }
  }

  def infiniteFactory(path: Long => Complex): Path = new Path {
    def maybeDuration: Option[Long] = None

    def apply(time: Long): Complex = path(time)
  }

  val positiveRealLine: Path = Path.infiniteFactory(Complex(_, 0))

  def positiveSegment(duration: Long): Path = positiveRealLine.stopAfter(duration)

  def segment(duration: Long, angle: Double, speed: Double): Path =
    positiveSegment(duration) * Complex.rotation(angle) * (speed / 1000)

  def goUp(duration: Long, speed: Double): Path    = segment(duration, math.Pi / 2, speed)
  def goDown(duration: Long, speed: Double): Path  = -goUp(duration, speed)
  def goRight(duration: Long, speed: Double): Path = positiveSegment(duration) * speed
  def goLeft(duration: Long, speed: Double): Path  = -goRight(duration, speed)

  def circleLoop(radius: Double, loopDuration: Long): Path =
    infiniteFactory(t => radius * Complex.rotation(2 * math.Pi * t / loopDuration))

  def circle(duration: Long, radius: Double): Path = circleLoop(radius, duration).stopAfter(duration)

}
