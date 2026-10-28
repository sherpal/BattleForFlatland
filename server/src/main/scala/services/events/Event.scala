package services.events

import menus.data.{AllGameCredentials, GameServerAddress, User}

trait Event

object Event {
  case class Ping() extends Event

  // Some(gameId) for a specific game, None for all
  case class GameDataRefreshed(maybeGameId: Option[String]) extends Event
  case class UserConnectedSocket(user: User, gameId: String) extends Event
  case class UserSocketDisconnected(user: User, gameId: String) extends Event
  case class GameStarted(gameId: String) extends Event

  /** The players of the game must send their candidacy to run the game server. */
  case class HostCandidacyRequested(gameId: String) extends Event

  /** This player must run the game server (with these arguments). */
  case class HostElected(gameId: String, host: String, gameSecret: String, rendezvousPort: Int)
      extends Event

  case class GameLaunchFailed(gameId: String, reason: String) extends Event

  /** Dispatched when the game server of a started game is ready.
    *
    * WebSocket connections then need to forward the correct user credentials to the clients, and
    * the addresses where they may reach the game server (the host, who runs it, may also reach it
    * through the loopback, on `gameServerPort`).
    */
  case class GameCredentials(
      creds: AllGameCredentials,
      gameServerAddresses: Vector[GameServerAddress],
      host: Option[String],
      gameServerPort: Int,
      rendezvousPort: Int
  ) extends Event
}
