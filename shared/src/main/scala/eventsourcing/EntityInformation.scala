package eventsourcing

import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag

/** Describes information about the evolution and metadata of an entity.
  *
  * @tparam Command
  *   type of messages sent to the entity
  * @tparam Event
  *   type of events that modify the entity
  * @tparam State
  *   current state of the entity
  */
trait EntityInformation[Command, Event, State] {

  /** Unique identifier for the kind of entities (typically: name of the class, if that's unique enough) */
  def entityKind: EntityKind[Command, State]

  /** Initial empty state for a fresh entity */
  def initialState: State

  /** Describes how an event modifies the state */
  def eventHandler(event: Event, state: State): State

  /** Describes how the entity must handle an upcoming command */
  def commandHandler(command: Command, state: State, entityId: Int): Effect[Event, State]

  /** Describes how to store the event in the event store */
  def serializeEvent(event: Event): String

  /** Describes how to decode an event from the event store */
  def deserializeEvent(payload: String): Event

  private[eventsourcing] def decodeEnvelope(envelope: RawEventEnvelope): EventEnvelope[Event, State] = {
    import envelope.*
    EventEnvelope(
      entityId = entityId,
      sequenceNumber = sequenceNumber,
      event = deserializeEvent(eventPayload),
      timestamp = Time.fromValue(timestamp)
    )
  }

  private[eventsourcing] def encodeEnvelope(envelope: EventEnvelope[Event, State]): RawEventEnvelope = {
    import envelope.*
    RawEventEnvelope(
      offset = 0, // ignored: the insert skips this column, the database assigns it
      entityId,
      sequenceNumber,
      eventPayload = serializeEvent(envelope.event),
      entityKind = entityKind.name,
      timestamp = envelope.timestamp.value
    )
  }

}

object EntityInformation {

  def usingCirceSerialization[Command, Event, State](
      initialState: State,
      eventHandler: (Event, State) => State,
      commandHandler: (Command, State, Int) => Effect[Event, State]
  )(using
      encoder: Encoder[Event],
      decoder: Decoder[Event],
      ct: ClassTag[State]
  ): EntityInformation[Command, Event, State] =
    val initialState0   = initialState
    val eventHandler0   = eventHandler
    val commandHandler0 = commandHandler
    new EntityInformation[Command, Event, State] {
      override def entityKind: EntityKind[Command, State] = EntityKind(ct.runtimeClass.getName)

      override def initialState: State = initialState0

      override def eventHandler(event: Event, state: State): State = eventHandler0(event, state)

      override def commandHandler(command: Command, state: State, entityId: Int): Effect[Event, State] =
        commandHandler0(command, state, entityId)

      override def serializeEvent(event: Event): String = encoder(event).noSpaces

      override def deserializeEvent(payload: String): Event = io.circe.parser.decode[Event](payload).toTry.get
    }

}
