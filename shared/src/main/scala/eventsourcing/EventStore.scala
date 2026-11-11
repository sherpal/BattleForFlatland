package eventsourcing

import scala.concurrent.Future

/** Abstracts over durable storage of the event log and projection checkpoints, so the rest of the
  * event-sourcing machinery ([[EventSourcedActor]], [[Supervisor]], [[ProjectionRunner]],
  * [[EventSourcingService]]) can be cross-compiled to any platform that can provide an implementation —
  * e.g. backed by JDBC/sqlite on the JVM, or by a remote HTTP/WS store from JS. Every operation is
  * asynchronous, since not every platform (JS in particular) can perform blocking database access.
  */
trait EventStore {

  /** Appends the given envelopes to the log, in order. `offset` on each input envelope is ignored — the
    * store assigns it.
    */
  def appendEnvelopes(envelopes: Vector[RawEventEnvelope]): Future[Unit]

  /** Loads up to `pageSize` envelopes for one entity, in increasing sequence-number order, whose
    * sequence number is strictly greater than `afterSequenceNumber`. Used to recover one entity's state.
    */
  def loadEnvelopes(
      entityKind: String,
      entityId: Int,
      afterSequenceNumber: Int,
      pageSize: Int
  ): Future[Vector[RawEventEnvelope]]

  /** Loads up to `pageSize` envelopes of one entity kind (across all entities), in increasing offset
    * order, whose offset is strictly greater than `afterOffset`. Used by projections to catch up on and
    * follow the log.
    */
  def loadEnvelopesByOffset(entityKind: String, afterOffset: Int, pageSize: Int): Future[Vector[RawEventEnvelope]]

  /** Biggest entity id for which an event has been registered for that entity kind, if any. */
  def lastEntityId(entityKind: String): Future[Option[Int]]

  /** Offset of the most recently appended envelope of that entity kind, if any. */
  def latestOffset(entityKind: String): Future[Option[Int]]

  /** Persisted checkpoint offset for a projection, if it has ever run. */
  def loadCheckpoint(projectionName: String): Future[Option[Int]]

  /** Overwrites the persisted checkpoint offset for a projection (upsert). */
  def saveCheckpoint(projectionName: String, offset: Int): Future[Unit]

  /** Resource cleanup hook. */
  def close(): Unit

}
