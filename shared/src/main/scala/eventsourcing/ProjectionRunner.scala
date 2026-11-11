package eventsourcing

import be.doeraene.utils.testshenanigans.OnlyInTest
import castor.SimpleActor

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/** Drives one [[Projection]]: polls the event log for events of one entity kind past the
  * projection's last saved checkpoint, applies them in order, and persists progress as it goes.
  *
  * One `ProjectionRunner` is one castor actor, so a given projection's events are always applied
  * strictly in order, one at a time — independent projections run fully in parallel with each other
  * and with the entity actors, since none of them share a mailbox.
  *
  * @param autoPoll
  *   whether to keep re-scheduling itself after catching up (production). Tests should pass `false`
  *   and drive catch-up explicitly via `poke()`, since a self-rescheduling timer would otherwise
  *   keep `castor.Context.Test` permanently "active" and make `waitForInactivity()` hang forever.
  */
private[eventsourcing] class ProjectionRunner[Event, EntityState](
    eventStore: EventStore,
    scheduler: Scheduler,
    entityInfo: EntityInformation[?, Event, EntityState],
    projection: Projection[Event],
    pollInterval: FiniteDuration,
    autoPoll: Boolean,
    supervisor: Supervisor
)(using castor.Context)
    extends castor.StateMachineActor[ProjectionRunner.Message] {
  import ProjectionRunner.*

  /** Runs the handler for one row, swallowing any exception (logging it) so a broken handler can
    * never take the whole actor down. Returns whether it succeeded.
    */
  private def runHandler(row: RawEventEnvelope): Boolean =
    try {
      projection.handle(row.entityId, entityInfo.decodeEnvelope(row))
      supervisor.send(Supervisor.ProjectionUpdate(projection.name, row.entityId))
      true
    } catch {
      case NonFatal(e) =>
        // TODO: route through a real logger instead, once one exists in this codebase.
        System.err.println(
          s"[projection ${projection.name}] handler failed for offset ${row.offset}: $e"
        )
        e.printStackTrace()
        false
    }

  private def processAtLeastOnce(rows: List[RawEventEnvelope], cursor: Int): Future[Int] =
    rows match {
      case row :: rest =>
        if runHandler(row) then
          eventStore
            .saveCheckpoint(projection.name, row.offset)
            .flatMap(_ => processAtLeastOnce(rest, row.offset))
        else
          Future.successful(
            cursor
          ) // stop here: this event (and the rest of the batch) is retried on the next tick
      case Nil => Future.successful(cursor)
    }

  private def processAtMostOnce(rows: List[RawEventEnvelope], cursor: Int): Future[Int] =
    rows match {
      case row :: rest =>
        eventStore.saveCheckpoint(projection.name, row.offset).flatMap {
          _ => // commit *before* handling: a
            // crash from here on just skips this event
            runHandler(row) // best-effort; failure or not, we never come back to it
            processAtMostOnce(rest, row.offset)
        }
      case Nil => Future.successful(cursor)
    }

  private def replyIsUpToDate(replyTo: castor.Actor[Boolean], cursor: Int): Unit =
    eventStore.latestOffset(entityInfo.entityKind.name).onComplete {
      case Success(latest) => replyTo.send(cursor >= latest.getOrElse(-1))
      case Failure(error) =>
        System.err
          .println(s"[projection ${projection.name}] failed to check up-to-date-ness: $error")
        error.printStackTrace()
        replyTo.send(false)
    }

  private sealed abstract class TheState(handler: Message => State) extends State(handler)

  /** Initial state: waiting for the persisted checkpoint to load before doing anything else,
    * buffering `IsUpToDate` queries in the meantime (mirrors [[EventSourcedActor]]'s recovery
    * buffering).
    */
  private case class LoadingCheckpoint(bufferedIsUpToDate: Vector[IsUpToDate])
      extends TheState({
        case Tick() => state // stray: the first Tick is fired once the checkpoint is known
        case IsUpToDate(replyTo) => LoadingCheckpoint(bufferedIsUpToDate :+ IsUpToDate(replyTo))
        case CheckpointLoaded(offset) =>
          send(Tick()) // kick off the first catch-up/delivery cycle immediately
          bufferedIsUpToDate.foreach(send)
          Idle(offset)
        case BatchProcessed(_, _) | BatchFailed(_) => state // unreachable here
      })

  private case class Idle(cursor: Int)
      extends TheState({
        case Tick() =>
          eventStore
            .loadEnvelopesByOffset(entityInfo.entityKind.name, cursor, projection.batchSize)
            .onComplete {
              case Success(batch) =>
                val processed = projection.semantics match {
                  case Projection.Semantics.AtLeastOnce => processAtLeastOnce(batch.toList, cursor)
                  case Projection.Semantics.AtMostOnce => processAtMostOnce(batch.toList, cursor)
                }
                processed.onComplete {
                  case Success(newCursor) =>
                    send(BatchProcessed(newCursor, batch.sizeIs == projection.batchSize))
                  case Failure(error) => send(BatchFailed(error))
                }
              case Failure(error) => send(BatchFailed(error))
            }
          Fetching(cursor, tickPending = false)
        case IsUpToDate(replyTo) =>
          replyIsUpToDate(replyTo, cursor)
          state
        case CheckpointLoaded(_) | BatchProcessed(_, _) | BatchFailed(_) =>
          state // unreachable here
      })

  /** A `Tick()` is in flight (its batch was fetched and/or is being processed). Further ticks are
    * coalesced rather than dropped, so a `poke()` landing mid-batch still forces one more catch-up
    * cycle once the current one finishes.
    */
  private case class Fetching(cursor: Int, tickPending: Boolean)
      extends TheState({
        case Tick() => Fetching(cursor, tickPending = true)
        case IsUpToDate(replyTo) =>
          replyIsUpToDate(replyTo, cursor)
          state
        case BatchProcessed(newCursor, batchWasFull) =>
          if batchWasFull || tickPending then {
            // there may be more waiting right behind it (or a poke() arrived meanwhile): keep catching up
            send(Tick())
            Fetching(newCursor, tickPending = false)
          } else if autoPoll then {
            scheduler.scheduleOnce(pollInterval)(() => send(Tick()))
            Idle(newCursor)
          } else
            Idle(
              newCursor
            ) // caught up, auto-polling off (test mode): sit idle until poke() is called
        case BatchFailed(error) =>
          // TODO: route through a real logger instead, once one exists in this codebase.
          System.err.println(
            s"[projection ${projection.name}] failed to fetch/process a batch: $error"
          )
          error.printStackTrace()
          Idle(cursor) // retried on the next tick (autoPoll timer or poke())
        case CheckpointLoaded(_) => state // unreachable here
      })

  override def initialState: State = LoadingCheckpoint(Vector.empty)

  eventStore.loadCheckpoint(projection.name).onComplete {
    case Success(offset) => send(CheckpointLoaded(offset.getOrElse(0)))
    case Failure(error) =>
      System.err.println(s"[projection ${projection.name}] failed to load checkpoint: $error")
      error.printStackTrace()
  }

  /** For tests: force one catch-up cycle, without waiting for the automatic poll timer. */
  private[eventsourcing] def poke(): Unit = send(Tick())
}

object ProjectionRunner {
  private[eventsourcing] sealed trait Message
  private[eventsourcing] case class Tick() extends Message
  private[eventsourcing] case class IsUpToDate(replyTo: castor.Actor[Boolean]) extends Message

  // -- internal continuations, only ever sent by a ProjectionRunner to itself --
  private[eventsourcing] case class CheckpointLoaded(offset: Int) extends Message
  private[eventsourcing] case class BatchProcessed(newCursor: Int, batchWasFull: Boolean)
      extends Message
  private[eventsourcing] case class BatchFailed(error: Throwable) extends Message

  /** Handle to a running projection. Only really useful in tests: `poke()` forces one catch-up
    * cycle immediately, instead of waiting for the automatic poll timer.
    */
  final class ProjectionHandle private[eventsourcing] (runner: ProjectionRunner[?, ?])(using
      castor.Context
  ) {
    def poke()(using OnlyInTest): Unit = runner.poke()

    def isUpToDate: Future[Boolean] = {
      val promise = scala.concurrent.Promise[Boolean]()
      val replyActor = new SimpleActor[Boolean]() {
        override def run(msg: Boolean): Unit = promise.success(msg)
      }
      runner.send(IsUpToDate(replyActor))
      promise.future
    }
  }

}
