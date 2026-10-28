package bffclient.game

import communication.udp.Peer

/** Credentials to connect to a game server, received from the menu server.
  *
  * @param addresses
  *   where the game server may be reached (the first one to answer wins)
  * @param rendezvous
  *   where to register for the UDP hole punching (see `communication.udp.Rendezvous`), if anywhere
  */
final case class GameAccess(
    addresses: Vector[Peer],
    rendezvous: Option[Peer],
    userName: String,
    gameId: String,
    secret: String
)
