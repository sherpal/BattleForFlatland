package eventsourcing

import eventsourcing.EventSourcedActor.ActorCommand

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{Failure, Success}

private[eventsourcing] class EventSourcedActor[Command, Event, EntityState](
    id: Int,
    entityInfo: EntityInformation[Command, Event, EntityState],
    eventStore: EventStore,
    config: EventSourcingService.Config,
    supervisor: castor.Actor[Supervisor.FromEventSourcedActor]
)(using castor.Context)
    extends castor.StateMachineActor[ActorCommand[Command]] {

  import entityInfo.*

  /** Applies one effect, persisting events (if any) through the [[EventStore]]. Only the `PersistMultiple` branch does
    * real asynchronous work; everything else resolves immediately, via an already-completed future.
    */
  private def handleEffect(
      lastSequenceNumber: Int,
      currentState: EntityState,
      effect: Effect[Event, EntityState]
  ): Future[(sequenceNumber: Int, state: EntityState)] = effect match {
    case Effect.Persist(event) =>
      handleEffect(lastSequenceNumber, currentState, Effect.PersistMultiple(Vector(event)))
    case Effect.PersistMultiple(events) =>
      val startingSequenceNumber = lastSequenceNumber + 1

      val nextState = events.foldLeft(currentState)((s, e) => eventHandler(e, s))
      val envelopes = events.zipWithIndex
        .map((event, index) => (event, startingSequenceNumber + index))
        .map((event, sequenceNumber) =>
          EventEnvelope[Event, EntityState](id, sequenceNumber, event, Time.now())
        )
      eventStore.appendEnvelopes(envelopes.map(encodeEnvelope)).map { _ =>
        val resolved =
          (sequenceNumber = envelopes.map(_.sequenceNumber).maxOption.getOrElse(lastSequenceNumber), state = nextState)
        supervisor.send(
          Supervisor.EntityUpdate(id, entityInfo.entityKind.name, resolved.state, resolved.sequenceNumber)
        )
        resolved
      }
    case Effect.WithSideEffect(effect, sideEffect) =>
      handleEffect(lastSequenceNumber, currentState, effect).map { resolved =>
        sideEffect(resolved.state)
        resolved
      }
    case Effect.Ignore() => Future.successful((sequenceNumber = lastSequenceNumber, state = currentState))
    case Effect.ReplyTo(replyTo, message) =>
      handleEffect(lastSequenceNumber, currentState, Effect.Ignore().thenReply(replyTo)(message))
  }

  /** Applies a backlog of buffered commands one at a time, exactly as if each had just arrived while `Loaded` — instead
    * of dumping them all back into the mailbox at once and transitioning straight to `Loaded`. That naive approach has
    * a real hazard: castor processes a mailbox in fixed-size batches (see `castor.BaseActor.runWithItems`, which
    * snapshots the queue once and runs every message in that snapshot before ever looking at the queue again), so a
    * command that happens to land in the *same* batch as the message that triggers the flush (the last, empty recovery
    * page; or a persisted effect completing) would get applied immediately against `Loaded` — jumping ahead of the
    * backlog, even though it logically arrived later.
    *
    * Draining one command at a time and never returning `Loaded` until `remaining` is truly empty avoids that: anything
    * batched alongside us just lands on a state that still buffers (`Processing`, via the `Wrapper`/`Passivate` cases
    * below), so it joins the back of the queue instead of racing ahead.
    */
  @tailrec
  private def drain(
      lastSequenceNumber: Int,
      entity: EntityState,
      passivating: Boolean,
      remaining: List[ActorCommand[Command]]
  ): State = remaining match {
    case Nil                                    => Loaded(lastSequenceNumber, entity, passivating)
    case ActorCommand.Wrapper(command) :: other =>
      handleEffect(lastSequenceNumber, entity, commandHandler(command, entity, id)).onComplete {
        case Success(resolved) => send(ActorCommand.CommandHandled(resolved))
        case Failure(error)    => send(ActorCommand.PersistFailed(error))
      }
      Processing(lastSequenceNumber, entity, passivating, other)
    case ActorCommand.Passivate() :: others =>
      if !passivating then
        // first time we see it: same "defer to the very end" trick as Loaded.Passivate, but done by
        // moving it within our own backlog instead of round-tripping through the real mailbox.
        drain(lastSequenceNumber, entity, passivating = true, others :+ ActorCommand.Passivate())
      else {
        supervisor.send(Supervisor.EntityIsNowPassive(id, entityKind.name))
        Passive()
      }
    case _ :: others => drain(lastSequenceNumber, entity, passivating, others) // stray: never buffered
  }

  private sealed abstract class TheState(handler: ActorCommand[Command] => State) extends State(handler)

  private case class LoadingState(
      currentSequenceNumber: Int,
      currentState: EntityState,
      eventsInQueue: Vector[ActorCommand[Command]]
  ) extends TheState({
        case ActorCommand.LoadNext() =>
          eventStore
            .loadEnvelopes(entityKind.name, id, currentSequenceNumber, config.eventPageSize)
            .onComplete {
              case Success(envelopes) => send(ActorCommand.RecoveryPageLoaded(envelopes))
              case Failure(error)     =>
                // TODO: route through a real logger instead, once one exists in this codebase. Same
                // known-fragility as a corrupted event payload: this leaves the entity permanently
                // stuck in its loading state rather than surfacing an error.
                System.err.println(s"[entity $id/${entityKind.name}] failed to load recovery page: $error")
                error.printStackTrace()
            }
          state
        case ActorCommand.RecoveryPageLoaded(rawEnvelopes) =>
          val nextEnvelopes = rawEnvelopes.map(decodeEnvelope)
          if nextEnvelopes.isEmpty then {
            drain(currentSequenceNumber, currentState, passivating = false, eventsInQueue.toList)
          } else {
            val newState =
              nextEnvelopes.foldLeft(currentState)((state, envelope) => eventHandler(envelope.event, state))
            val nextSequenceNumber = nextEnvelopes.last.sequenceNumber
            send(ActorCommand.LoadNext())
            LoadingState(nextSequenceNumber, newState, eventsInQueue)
          }
        case ActorCommand.Wrapper(command) =>
          LoadingState(currentSequenceNumber, currentState, eventsInQueue :+ ActorCommand.Wrapper(command))
        case ActorCommand.Passivate() =>
          // too soon to passivate, we wait for later
          LoadingState(currentSequenceNumber, currentState, eventsInQueue :+ ActorCommand.Passivate())
        case ActorCommand.CommandHandled(_) | ActorCommand.PersistFailed(_) => state // unreachable here
      })

  private case class Loaded(lastSequenceNumber: Int, entity: EntityState, passivating: Boolean)
      extends TheState({
        case ActorCommand.LoadNext()       => state
        case ActorCommand.Wrapper(command) =>
          handleEffect(lastSequenceNumber, entity, commandHandler(command, entity, id)).onComplete {
            case Success(resolved) => send(ActorCommand.CommandHandled(resolved))
            case Failure(error)    => send(ActorCommand.PersistFailed(error))
          }
          Processing(lastSequenceNumber, entity, passivating, queued = List.empty)
        case ActorCommand.Passivate() =>
          if !passivating then {
            // need to put back passivate at end of queue. In this state we know this has to be last message now
            send(ActorCommand.Passivate())
            Loaded(lastSequenceNumber, entity, passivating = true)
          } else
            supervisor.send(Supervisor.EntityIsNowPassive(id, entityKind.name))
            Passive()
        case ActorCommand.RecoveryPageLoaded(_) | ActorCommand.CommandHandled(_) | ActorCommand.PersistFailed(_) =>
          state // unreachable here
      })

  /** Buffers incoming commands (and passivation requests) while a command's effect is being applied asynchronously —
    * mirrors [[LoadingState]]'s buffering of commands arriving mid-recovery.
    */
  private case class Processing(
      lastSequenceNumber: Int,
      entity: EntityState,
      passivating: Boolean,
      queued: List[ActorCommand[Command]]
  ) extends TheState({
        case ActorCommand.LoadNext()       => state
        case ActorCommand.Wrapper(command) =>
          Processing(lastSequenceNumber, entity, passivating, queued :+ ActorCommand.Wrapper(command))
        case ActorCommand.Passivate() =>
          Processing(lastSequenceNumber, entity, passivating, queued :+ ActorCommand.Passivate())
        case ActorCommand.CommandHandled(result) =>
          val (nextSequenceNumber, nextState) = result.asInstanceOf[(Int, EntityState)]
          drain(nextSequenceNumber, nextState, passivating, queued)
        case ActorCommand.PersistFailed(error) =>
          // TODO: route through a real logger instead, once one exists in this codebase. Same
          // known-fragility as a corrupted event payload: this leaves the entity permanently stuck,
          // buffering further commands, rather than surfacing an error.
          System.err.println(s"[entity $id/${entityKind.name}] failed to persist effect: $error")
          error.printStackTrace()
          state
        case ActorCommand.RecoveryPageLoaded(_) => state // unreachable here
      })

  private case class Passive()
      extends TheState({ command =>
        println(s"Received this command, weird ($id): $command")
        state
      })

  override def initialState: EventSourcedActor.this.State = LoadingState(0, entityInfo.initialState, Vector.empty)
}

private[eventsourcing] object EventSourcedActor {

  private[eventsourcing] enum ActorCommand[Command]:
    case LoadNext[C]()          extends ActorCommand[C]
    case Wrapper[C](command: C) extends ActorCommand[C]
    case Passivate[C]()         extends ActorCommand[C]

    // -- internal continuations, only ever sent by an EventSourcedActor to itself --

    /** One page of recovery events came back from the store (possibly empty, meaning recovery is done). */
    private[EventSourcedActor] case RecoveryPageLoaded[C](envelopes: Vector[RawEventEnvelope]) extends ActorCommand[C]

    /** A command's effect finished being applied. `result` is really `(Int, EntityState)` for the enclosing
      * [[EventSourcedActor]] instance — `EntityState` can't be a type parameter here, since [[Supervisor]] holds actors
      * of this type uniformly across many different concrete `EntityState`s. This case is only ever constructed and
      * consumed by the very instance that sent it, so the cast back in [[EventSourcedActor.Processing]] is safe.
      */
    private[EventSourcedActor] case CommandHandled[C](result: (Int, Any)) extends ActorCommand[C]

    /** A command's effect failed to persist. */
    private[EventSourcedActor] case PersistFailed[C](error: Throwable) extends ActorCommand[C]

}
