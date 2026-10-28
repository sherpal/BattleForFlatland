package server

import communication.udp.{Peer, Rendezvous, UDPSocket}
import communication.udp.Rendezvous.Packet
import menus.data.AllGameCredentials

/** The rendezvous of the UDP hole punching (see `communication.udp.Rendezvous`): remembers where
  * the game servers and the players of the started games are seen from, and tells the game servers
  * where their players are.
  *
  * Only the game servers and players with the credentials of their game are considered.
  *
  * @param credentials
  *   the credentials of a started game (it may block)
  */
final class RendezvousServer(
    socket: UDPSocket,
    credentials: String => Option[AllGameCredentials]
) {
  import RendezvousServer.*

  private case class Seen(peer: Peer, at: Long)
  private case class Game(server: Option[Seen], players: Map[String, Seen])

  private val lock                                              = new Object
  private var games                                             = Map.empty[String, Game]
  private var knownCredentials: Map[String, AllGameCredentials] = Map.empty

  def start(port: Int, address: Option[String]): Unit = {
    println(s"Starting rendezvous server on port $port")
    socket.setMessageCallback(receive)
    socket.bind(port, address)
  }

  /** Where the game server of this game was seen from (lately), if it was. */
  def gameServerAddress(gameId: String): Option[Peer] =
    lock.synchronized(games.get(gameId).flatMap(_.server).map(_.peer))

  private def credentialsOf(gameId: String): Option[AllGameCredentials] =
    lock.synchronized(knownCredentials.get(gameId)).orElse {
      val found = credentials(gameId)
      found.foreach(creds => lock.synchronized(knownCredentials += gameId -> creds))
      found
    }

  private def reply(to: Peer, packet: Packet): Unit =
    socket.send(Rendezvous.encode(packet), to.port, to.address)

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    Rendezvous.parse(datagram).foreach {
      case Packet.ServerRegister(gameId, gameSecret)
          if credentialsOf(gameId).exists(_.secret == gameSecret) =>
        val players = seen(gameId, game => game.copy(server = Some(Seen(from, now))))
          .fold(Vector.empty)(_.players.toVector.map((name, seen) => name -> seen.peer))
        reply(from, Packet.Players(players))
      case Packet.PlayerRegister(userCredentials)
          if credentialsOf(userCredentials.gameId)
            .exists(_.allGameUserCredentials.contains(userCredentials)) =>
        seen(
          userCredentials.gameId,
          game => game.copy(players = game.players + (userCredentials.userName -> Seen(from, now)))
        )
        reply(from, Packet.Registered(from))
      case _ => () // not for us, or not authorized
    }

  private def now: Long = System.currentTimeMillis()

  /** Updates the game, and forgets what was not seen for too long. */
  private def seen(gameId: String, update: Game => Game): Option[Game] = lock.synchronized {
    val limit = now - Forget
    games = games.updatedWith(gameId)(game => Some(update(game.getOrElse(Game(None, Map.empty)))))
    games = games.view
      .mapValues(game =>
        Game(game.server.filter(_.at > limit), game.players.filter(_._2.at > limit))
      )
      .filter((_, game) => game.server.isDefined || game.players.nonEmpty)
      .toMap
    knownCredentials = knownCredentials.filter((gameId, _) => games.contains(gameId))
    games.get(gameId)
  }
}

object RendezvousServer {

  /** The game servers say hello every second, the players every half second while connecting. */
  private val Forget = 60000L
}
