package services.events

import menus.data.User
import menus.data.AllGameCredentials

trait Event

object Event {
  case class Ping() extends Event

  // Some(gameId) for a specific game, None for all
  case class GameDataRefreshed(maybeGameId: Option[String])     extends Event
  case class UserConnectedSocket(user: User, gameId: String)    extends Event
  case class UserSocketDisconnected(user: User, gameId: String) extends Event
  case class GameStarted(gameId: String)                        extends Event

  /** Dispatched when a game was started and credentials for each player have been generated
    *
    * WebSocket connections then need to forward the correct user credentials to the clients
    */
  case class GameCredentials(creds: AllGameCredentials, gameServerPort: Int) extends Event
}
