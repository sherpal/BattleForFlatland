package eventsourcing

import be.doeraene.utils.testshenanigans.OnlyInTest
import eventsourcing.EventSourcingService.Subscription

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class EventSourcingService(
    config: EventSourcingService.Config,
    eventStore: EventStore,
    scheduler: Scheduler,
    isInTest: Boolean = false
)(using
    castor.Context
) {

  private val supervisor =
    Supervisor(
      eventStore,
      scheduler,
      config = config.copy(removeIdleEntities = !isInTest && config.removeIdleEntities)
    )

  def entity[Command, Event, State](
      id: Int,
      info: EntityInformation[Command, Event, State]
  ): castor.Actor[Command] =
    case class Entity(id: Int, kind: EntityKind[Command, ?])
        extends castor.ProxyActor[Command, Supervisor.SupervisorMessage](
          Supervisor.EntityCommand(id, info, _),
          supervisor
        )
    Entity(id, info.entityKind)

  private val subscriptionCount = AtomicInteger()

  /** Calling this function will make it so that the specified `replyTo` actor will receive a
    * message every time the entity changes
    * @param id
    *   entity id to listen to
    * @param entityKind
    *   kind of entity
    * @param replyTo
    *   actor that will receive the updates
    * @return
    *   a subscription that can be called (and should be in due time) to stop receiving the
    *   notifications.
    */
  def subscribe[State](
      id: Int,
      entityKind: EntityKind[?, State],
      replyTo: castor.Actor[EntityUpdateNotification[State]]
  )(using tp: scala.reflect.Typeable[State]): Subscription = {
    val subscriptionName = s"subscription-${subscriptionCount.getAndIncrement()}"
    supervisor.send(Supervisor.Subscribe(subscriptionName, id, entityKind, replyTo, tp))
    Subscription(() => supervisor.send(Supervisor.Unsubscribe(id, entityKind, subscriptionName)))
  }

  /** Calling this function will make it so that the specified `replyTo` actor will receive a
    * message every time the projection handles an event. The message sent is the entity of the id
    * who triggered the event.
    *
    * @param projectionName
    *   name of the projection to listen to (not guarded against spelling mistake!)
    * @param replyTo
    *   actor that will receive the updates
    * @return
    *   a subscription that can be called (and should be in due time) to stop receiving the
    *   notifications.
    */
  def subscribeToProjection(
      projectionName: String,
      replyTo: castor.Actor[Int]
  ): Subscription = {
    val subscriptionName = s"projection-subscription-${subscriptionCount.getAndIncrement()}"
    supervisor.send(Supervisor.SubscribeToProjection(subscriptionName, projectionName, replyTo))
    Subscription(() => supervisor.send(Supervisor.UnsubscribeFromProjection(subscriptionName)))
  }

  /** Returns the biggest entity id for which an event has been registered.
    *
    * If you want to ensure that you don't create an entity with the same id "twice", you should
    * have a "Created" state in your entity, and a first command "Create". That command should then
    * check if the entity is already created, and return a failure if that's the case.
    *
    * @param entityKind
    *   kind of entity for which you want the last id.
    */
  def lastEntityId(entityKind: EntityKind[?, ?]): Future[Option[Int]] =
    eventStore.lastEntityId(entityKind.name)

  /** Registers a [[Projection]] against the events of one entity kind and starts it running
    * immediately. If the log already has events for that kind, they are delivered first, in order,
    * exactly as if they were happening live — there is no separate backfill step.
    *
    * The returned handle only matters for tests (`poke()` forces a catch-up cycle without waiting
    * on the automatic poll timer); production code can otherwise discard it.
    */
  def registerProjection[Event, State](
      entityInfo: EntityInformation[?, Event, State],
      projection: Projection[Event],
      pollInterval: FiniteDuration = config.projectionPollInterval
  ): ProjectionRunner.ProjectionHandle = {
    val registeredProjections =
      registeredProjectionsRef.getAndUpdate(prev => prev + projection.name)
    if registeredProjections.contains(projection.name) then
      throw IllegalArgumentException(
        s"Projection with name ${projection.name} is already registered"
      )
    ProjectionRunner.ProjectionHandle(
      ProjectionRunner(
        eventStore,
        scheduler,
        entityInfo,
        projection,
        pollInterval,
        autoPoll = !isInTest,
        supervisor
      )
    )
  }

  private[eventsourcing] def cleanRegisteredProjection(name: String)(using OnlyInTest): Unit =
    registeredProjectionsRef.getAndUpdate(prev => prev - name)
    ()

  private[eventsourcing] def clearMemory()(using OnlyInTest): Unit =
    supervisor.send(Supervisor.ClearMemory())

  private[eventsourcing] def closeDb()(using OnlyInTest): Unit = eventStore.close()

  private val registeredProjectionsRef = AtomicReference[Set[String]](Set.empty)

}

object EventSourcingService {

  case class Config(
      maxInMemoryEntities: Int = 2000,
      entityIdleShutdownTime: FiniteDuration = 1.hour,
      eventPageSize: Int = 50,
      removeIdleEntities: Boolean = true,
      projectionPollInterval: FiniteDuration = 200.millis
  )

  object Config:
    def default: Config = Config()

  opaque type Subscription = () => Unit
  object Subscription {
    extension (sub: Subscription) {
      inline def unsubscribe(): Unit = sub()

      inline def asFunction: () => Unit = sub
    }

    private[EventSourcingService] def apply(unsub: () => Unit): Subscription = unsub
  }

}
