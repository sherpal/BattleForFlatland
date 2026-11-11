package eventsourcing

/** Plain, storage-agnostic representation of one persisted event, as read from or written to an
  * [[EventStore]]. `offset` is a global, gapless, per-store insertion order used by projections; it is
  * assigned by the store itself, and should be treated as opaque/ignored when appending new envelopes.
  */
case class RawEventEnvelope(
    offset: Int,
    entityId: Int,
    sequenceNumber: Int,
    eventPayload: String,
    entityKind: String,
    timestamp: Long
)
