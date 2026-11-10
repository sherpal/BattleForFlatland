package server

import services.events.Events
import services.hostelections.HostElections
import services.localstorage.{BLocalStorage, LocalStorage}
import services.menugames.MenuGames

/** The services of the menu server. */
final case class BackendEnv(
    storage: LocalStorage,
    menuGames: MenuGames,
    events: Events,
    hostElections: HostElections
)

object BackendEnv {
  def live(): BackendEnv = {
    val storage = BLocalStorage.live()
    BackendEnv(storage, MenuGames(storage), Events(), HostElections())
  }
}
