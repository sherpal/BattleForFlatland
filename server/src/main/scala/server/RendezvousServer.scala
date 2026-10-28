package server

import communication.udp.{Peer, Rendezvous, UDPSocket}
import communication.udp.Rendezvous.Packet
import menus.data.AllGameCredentials

/** The rendezvous of the UDP hole punching (see `communication.udp.Rendezvous`): remembers where
  * the game servers and the players of the started games are seen from, and tells the game servers
  * where their players are. For the ones seen on a private network, it also remembers the public
  * address they say they have (see `communication.udp.Stun`).
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

  /** `peers`: where it was seen from, and its public address if it gave one. */
  private case class Seen(peers: Vector[Peer], at: Long)
  private case class Game(server: Option[Seen], players: Map[String, Seen])

  private val lock                                              = new Object
  private var games                                             = Map.empty[String, Game]
  private var knownCredentials: Map[String, AllGameCredentials] = Map.empty

  def start(port: Int, address: Option[String]): Unit = {
    println(s"Starting rendezvous server on port $port")
    socket.setMessageCallback(receive)
    socket.bind(port, address)
  }

  /** Where the game server of this game was seen from (lately), and its public address if it gave
    * one, if it was seen.
    */
  def gameServerAddresses(gameId: String): Vector[Peer] =
    lock.synchronized(games.get(gameId).flatMap(_.server).fold(Vector.empty)(_.peers))

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
      case Packet.ServerRegister(gameId, gameSecret, publicAddress)
          if credentialsOf(gameId).exists(_.secret == gameSecret) =>
        val registered = Packet.Registered(from, publicAddress.filter(_ => from.onPrivateNetwork))
        val players = seen(gameId, game => game.copy(server = Some(seenAs(registered))))
          .fold(Vector.empty)(_.players.toVector.flatMap((name, seen) => seen.peers.map(name -> _)))
        reply(from, Packet.Players(registered, players))
      case Packet.PlayerRegister(userCredentials, publicAddress)
          if credentialsOf(userCredentials.gameId)
            .exists(_.allGameUserCredentials.contains(userCredentials)) =>
        val registered = Packet.Registered(from, publicAddress.filter(_ => from.onPrivateNetwork))
        seen(
          userCredentials.gameId,
          game =>
            game.copy(players = game.players + (userCredentials.userName -> seenAs(registered)))
        )
        reply(from, registered)
      case _ => () // not for us, or not authorized
    }

  private def seenAs(registered: Packet.Registered): Seen =
    Seen((registered.seenAs +: registered.publicAddress.toVector).distinct, now)

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
