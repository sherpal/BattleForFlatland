package server

import eventsourcing.{CastorScheduler, EventSourcingService, SqlEventStore}
import services.database.DatabaseService
import services.events.Events
import services.hostelections.HostElections
import services.menugames.MenuGames

/** The services of the menu server. */
final case class BackendEnv(
    database: DatabaseService,
    eventSourcing: EventSourcingService,
    menuGames: MenuGames,
    events: Events,
    hostElections: HostElections
)

object BackendEnv {
  def live(): BackendEnv = {
    given castor.Context = castor.Context.Simple.global
    println("Initializing database...")
    val database = DatabaseService((os.pwd / "target" / "db").toNIO)
    val eventSourcing = EventSourcingService(
      EventSourcingService.Config.default,
      SqlEventStore(database.client),
      CastorScheduler()
    )
    BackendEnv(database, eventSourcing, MenuGames(eventSourcing, database), Events(), HostElections())
  }
}
