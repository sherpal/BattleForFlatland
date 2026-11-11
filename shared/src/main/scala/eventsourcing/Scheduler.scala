package eventsourcing

import scala.concurrent.duration.FiniteDuration

/** Abstracts over "run this action once, after a delay" — the one piece of platform-specific timing
  * [[Supervisor]] (idle-entity sweep) and [[ProjectionRunner]] (poll timer) need. `castor.Context` itself
  * can't be used directly for this: `scheduleMsg` is only available on castor's JVM build, not its JS
  * build (true as of the latest published version, 0.3.2, on both platforms).
  */
trait Scheduler {
  def scheduleOnce(delay: FiniteDuration)(action: () => Unit): Unit
}
