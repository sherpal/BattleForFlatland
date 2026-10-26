package bffclient.render

/** The frame rate, averaged over the last `frames` frames (`love.timer.getFPS` averages over the
  * last second).
  */
final class FrameRate(frames: Int = 100) {
  private val durations = new Array[Double](frames)
  private var count     = 0
  private var total     = 0.0
  private var last      = Option.empty[Double]

  /** Once per frame, with the current time in seconds. */
  def tick(now: Double): Unit = {
    for previous <- last do {
      val i = count % frames
      total += (now - previous) - durations(i)
      durations(i) = now - previous
      count += 1
    }
    last = Some(now)
  }

  /** Frames per second, once there was a frame. */
  def fps: Option[Double] = Option.when(total > 0)(math.min(count, frames) / total)
}
