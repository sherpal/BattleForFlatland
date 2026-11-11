package eventsourcing

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.FiniteDuration

/** JVM-only [[Scheduler]] implementation, backed by `castor.Context.scheduleMsg` (a capability that
  * only exists on castor's JVM build). Reuses castor's own scheduling machinery via a throwaway
  * one-shot actor, so a scheduled action is tracked the same way any other actor message would be
  * (relevant for `castor.Context.Test.waitForInactivity()`, even though in practice neither
  * `Supervisor` nor `ProjectionRunner` ever schedule anything while `isInTest = true`).
  */
final class CastorScheduler(using ctx: castor.Context) extends Scheduler {
  override def scheduleOnce(delay: FiniteDuration)(action: () => Unit): Unit = {
    val actor = new castor.SimpleActor[Unit] {
      override def run(msg: Unit): Unit = action()
    }
    ctx.scheduleMsg(actor, (), java.time.Duration.of(delay.toMillis, ChronoUnit.MILLIS))
  }
}
