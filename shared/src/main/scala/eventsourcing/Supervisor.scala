package eventsourcing

private[eventsourcing] class Supervisor(
    eventStore: EventStore,
    scheduler: Scheduler,
    config: EventSourcingService.Config
)(using castor.Context)
    extends castor.StateMachineActor[Supervisor.SupervisorMessage] {
  import Supervisor.*

  override def toString: String = "EventSourcingSupervisor"

  private case class Subscription[T](
      name: String,
      ref: castor.Actor[EntityUpdateNotification[T]],
      tp: scala.reflect.Typeable[T]
  ) {
    private given scala.reflect.Typeable[T] = tp

    def send(update: EntityUpdateNotification[?]): Unit = update.state match {
      case t: T => ref.send(EntityUpdateNotification(t, update.sequenceNumber))
      case _    =>
        // todo: better logging
        println(s"Subscription was asked to send update of state ${update.state.getClass.getName}, which is weird.")
        ()
    }
  }

  private case class ProjectionSubscription(
      name: String,
      ref: castor.Actor[Int]
  ) {
    def poke(entityId: Int): Unit = ref.send(entityId)
  }

  private def handleCommandEnvelope(commandEnvelope: EntityCommand[?, ?, ?], currentState: TheState): TheState = {
    val id         = commandEnvelope.id
    val entityInfo = commandEnvelope.entityInfo
    val command    = commandEnvelope.command

    val TheState(entities, subscriptions, projectionSubscriptions) = currentState

    val entityType = entityInfo.entityKind.name
    entities.get((id, entityType)) match {
      case None =>
        val actor =
          EventSourcedActor(
            id,
            entityInfo,
            eventStore,
            config,
            castor.ProxyActor[Supervisor.FromEventSourcedActor, SupervisorMessage](identity, this)
          )
        actor.send(EventSourcedActor.ActorCommand.LoadNext())
        actor.send(EventSourcedActor.ActorCommand.Wrapper(command))

        val trimmedEntities = if entities.size >= config.maxInMemoryEntities then {
          val liveEntities = entities.toVector.collect { case (value, slot: EntitySlot.Live) => (value, slot) }
          liveEntities.minByOption(_._2.lastAccessed) match {
            case Some(toPassivate) =>
              toPassivate._2.actor.send(EventSourcedActor.ActorCommand.Passivate())
              entities.updated(toPassivate._1, EntitySlot.Passivating(Vector.empty))
            case None => entities
          }
        } else entities

        TheState(
          trimmedEntities + ((id = id, entityType = entityType) -> EntitySlot.Live(
            actor = actor.asInstanceOf[castor.Actor[eventsourcing.EventSourcedActor.ActorCommand[?]]],
            lastAccessed = System.currentTimeMillis()
          )),
          subscriptions,
          projectionSubscriptions
        )
      case Some(EntitySlot.Live(actor, _)) =>
        actor.send(EventSourcedActor.ActorCommand.Wrapper(command))
        TheState(
          entities.updated((id, entityType), EntitySlot.Live(actor, System.currentTimeMillis())),
          subscriptions,
          projectionSubscriptions
        )
      case Some(EntitySlot.Passivating(commands)) =>
        TheState(
          entities.updated((id, entityType), EntitySlot.Passivating(commands :+ commandEnvelope)),
          subscriptions,
          projectionSubscriptions
        )
    }

  }

  private case class TheState(
      entities: Map[(id: Int, entityType: String), EntitySlot],
      subscriptions: Map[(id: Int, entityType: String), Vector[Subscription[?]]],
      projectionSubscriptions: Map[String, Vector[ProjectionSubscription]]
  ) extends State({
        case commandEnvelope @ EntityCommand(_, _, _) =>
          handleCommandEnvelope(commandEnvelope, TheState(entities, subscriptions, projectionSubscriptions))
        case EntityIsNowPassive(id, entityType) =>
          val commandsToHandle = entities.get((id, entityType)) match {
            case None                                   => Vector.empty
            case Some(EntitySlot.Live(_, _))            => Vector.empty
            case Some(EntitySlot.Passivating(commands)) => commands
          }

          commandsToHandle.foldLeft(
            TheState(entities.removed((id, entityType)), subscriptions, projectionSubscriptions)
          )((accState, nextCommand) => handleCommandEnvelope(nextCommand, accState))
        case EntityUpdate(id, entityType, entity, sequenceNumber) =>
          subscriptions
            .getOrElse((id, entityType), Vector.empty)
            .foreach(_.send(EntityUpdateNotification(entity, sequenceNumber)))
          state
        case Subscribe(name, id, entityType, ref, tp) =>
          TheState(
            entities,
            subscriptions.updatedWith((id, entityType.name)) {
              case None           => Some(Vector(Subscription(name, ref, tp)))
              case Some(existing) => Some(existing :+ Subscription(name, ref, tp))
            },
            projectionSubscriptions
          )
        case Unsubscribe(id, entityType, name) =>
          TheState(
            entities,
            subscriptions.updatedWith((id, entityType.name)) {
              case None           => None
              case Some(existing) =>
                val filtered = existing.filterNot(_.name == name)
                Option.when(filtered.nonEmpty)(filtered)
            },
            projectionSubscriptions
          )
        case CheckIdleEntities() =>
          val (idleEntities, activeEntities) = entities.partitionMap {
            case (id, passivating: EntitySlot.Passivating)     => Right((id, passivating))
            case (id, live @ EntitySlot.Live(_, lastAccessed)) =>
              Either.cond(
                (System.currentTimeMillis() - lastAccessed) <= config.entityIdleShutdownTime.toMillis,
                (id, live),
                (id, live)
              )
          }

          idleEntities.map(_._2.actor).foreach(_.send(EventSourcedActor.ActorCommand.Passivate()))
          scheduleCheckIdleEntities()
          TheState(
            activeEntities.toMap ++ idleEntities.map((id, _) => (id, EntitySlot.Passivating(Vector.empty))),
            subscriptions,
            projectionSubscriptions
          )
        case ClearMemory() =>
          val newEntities = entities.map {
            case (id, passivating: EntitySlot.Passivating) => id -> passivating
            case (id, active: EntitySlot.Live)             =>
              active.actor.send(EventSourcedActor.ActorCommand.Passivate())
              id -> EntitySlot.Passivating(Vector.empty)
          }

          TheState(newEntities, subscriptions, projectionSubscriptions)
        case SubscribeToProjection(name, projName, ref) =>
          TheState(
            entities,
            subscriptions,
            projectionSubscriptions.updatedWith(projName) {
              case None       => Some(Vector(ProjectionSubscription(name, ref)))
              case Some(subs) => Some(subs :+ ProjectionSubscription(name, ref))
            }
          )
        case UnsubscribeFromProjection(name) =>
          TheState(
            entities,
            subscriptions,
            projectionSubscriptions.toVector
              .map((projName, subs) => projName -> subs.filterNot(_.name == name))
              .filter(_._2.nonEmpty)
              .toMap
          )
        case ProjectionUpdate(name, entityId) =>
          projectionSubscriptions.getOrElse(name, Vector.empty).foreach(_.poke(entityId))
          state
      })

  override def initialState: State = TheState(Map.empty, Map.empty, Map.empty)

  private def scheduleCheckIdleEntities(): Unit =
    scheduler.scheduleOnce(config.entityIdleShutdownTime / 2)(() => send(Supervisor.CheckIdleEntities()))

  if config.removeIdleEntities then {
    println(s"Launching idle entities routine.")
    scheduleCheckIdleEntities()
  }
}

private[eventsourcing] object Supervisor {
  private enum EntitySlot:
    case Live(actor: castor.Actor[EventSourcedActor.ActorCommand[?]], lastAccessed: Long)
    case Passivating(bufferedCommands: Vector[EntityCommand[?, ?, ?]])

  sealed trait SupervisorMessage

  case class EntityCommand[Command, Event, State](
      id: Int,
      entityInfo: EntityInformation[Command, Event, State],
      command: Command
  ) extends SupervisorMessage

  sealed trait FromEventSourcedActor extends SupervisorMessage

  case class EntityIsNowPassive(id: Int, entityType: String) extends FromEventSourcedActor

  case class EntityUpdate[State](id: Int, entityType: String, state: State, sequenceNumber: Int)
      extends FromEventSourcedActor

  case class Subscribe[State](
      name: String,
      id: Int,
      entityType: EntityKind[?, State],
      ref: castor.Actor[EntityUpdateNotification[State]],
      tp: scala.reflect.Typeable[State]
  ) extends SupervisorMessage

  case class Unsubscribe[State](id: Int, entityType: EntityKind[?, State], name: String) extends SupervisorMessage

  case class SubscribeToProjection(
      subscriptionName: String,
      projectionName: String,
      ref: castor.Actor[Int]
  ) extends SupervisorMessage

  case class UnsubscribeFromProjection(subscriptionName: String) extends SupervisorMessage

  case class ProjectionUpdate(name: String, entityId: Int) extends SupervisorMessage

  private case class CheckIdleEntities() extends SupervisorMessage
  case class ClearMemory()               extends SupervisorMessage

}
