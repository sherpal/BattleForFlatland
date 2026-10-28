package menus.data

import io.circe.Codec

sealed trait MenuGameComm

/** What the menu server pushes on the lobby websocket.
  *
  * When the game is launched, the game server is run by one of the players: everyone is asked for
  * their [[HostCandidacy]] ([[MenuGameComm.HostCandidacyRequested]]), the menu server chooses
  * ([[MenuGameComm.HostElected]]), and tells the chosen one to run the game server
  * ([[MenuGameComm.SpawnGameServer]]). Once it is ready, everyone receives where to reach it
  * ([[MenuGameComm.HereAreYourCredentials]]).
  */
object MenuGameComm {
  case class DataUpdated() extends MenuGameComm
  case class GameStarted() extends MenuGameComm

  /** Answer with a [[HostCandidacy]] (`api/bff/host-candidacy`). */
  case class HostCandidacyRequested() extends MenuGameComm

  case class HostElected(userName: String) extends MenuGameComm

  /** You were chosen to run the game server (with these arguments). */
  case class SpawnGameServer(gameId: String, gameSecret: String, rendezvousPort: Int)
      extends MenuGameComm

  /** The game could not start; it can be launched again. */
  case class GameLaunchFailed(reason: String) extends MenuGameComm

  /** The game server may be reached at any of `gameServerAddresses` (the first one to answer wins),
    * after registering at the rendezvous (the menu server's host, on `rendezvousPort`), see
    * `communication.udp.Rendezvous`.
    */
  case class HereAreYourCredentials(
      gameId: String,
      secret: String,
      gameServerAddresses: Vector[GameServerAddress],
      rendezvousPort: Int
  ) extends MenuGameComm

  given Codec[MenuGameComm] = io.circe.generic.semiauto.deriveCodec

}
