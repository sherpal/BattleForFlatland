package services.events

import menus.data.User

trait Event

object Event {
  case class Ping() extends Event

  // Some(gameId) for a specific game, None for all
  case class GameDataRefreshed(maybeGameId: Option[String])     extends Event
  case class UserConnectedSocket(user: User, gameId: String)    extends Event
  case class UserSocketDisconnected(user: User, gameId: String) extends Event
}
