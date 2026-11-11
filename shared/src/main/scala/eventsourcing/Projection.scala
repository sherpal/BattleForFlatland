package eventsourcing

/** Describes a read-side side effect that reacts to the events of one entity kind, as they are
  * persisted.
  *
  * A projection is registered once (see [[EventSourcingService.registerProjection]]) and then
  * runs on its own, independently of any entity actor: it polls the event log in commit order,
  * starting wherever it last left off (its checkpoint, persisted in `projection_checkpoint`).
  * Registering a projection against a log that already has thousands of events needs no special
  * "backfill" handling — the checkpoint just starts at 0, and the same loop that catches it up
  * also delivers new events as they arrive.
  */
trait Projection[Event] {

  /** Stable identifier for this projection, used as the key of its persisted checkpoint.
    * Changing it effectively resets the projection to the beginning of the log.
    */
  def name: String

  def semantics: Projection.Semantics

  /** How many events to read from the log per database round-trip. */
  def batchSize: Int = 100

  /** Called once per event, in commit order (never out of order, never concurrently with itself
    * — a projection's own events are always applied one at a time).
    *
    * How a thrown exception is handled depends on [[semantics]]: under
    * [[Projection.Semantics.AtLeastOnce]] it stops the current batch, and this same event (and
    * anything after it) is retried on the next tick; under [[Projection.Semantics.AtMostOnce]]
    * it is logged and the event is skipped for good, since the checkpoint has already moved
    * past it.
    */
  def handle(entityId: Int, envelope: EventEnvelope[Event, ?]): Unit

}

object Projection {

  enum Semantics {

    /** Every selected event is guaranteed to reach [[Projection.handle]] at least once, but a
      * crash between running the handler and persisting the checkpoint can make it run again
      * for the same event — handlers must be idempotent (or otherwise tolerant of re-delivery).
      */
    case AtLeastOnce

    /** [[Projection.handle]] never runs twice for the same event, but a crash (or the handler
      * itself throwing) can cause an event to never reach it at all.
      */
    case AtMostOnce
  }

  /** Convenience constructor for the common case of a plain function handler. */
  def apply[Event](
      name0: String,
      semantics0: Projection.Semantics,
      batchSize0: Int = 100
  )(handle0: (Int, EventEnvelope[Event, ?]) => Unit): Projection[Event] =
    new Projection[Event] {
      def name: String         = name0
      def semantics: Semantics = semantics0
      override def batchSize: Int = batchSize0
      def handle(entityId: Int, envelope: EventEnvelope[Event, ?]): Unit = handle0(entityId, envelope)
    }

}
