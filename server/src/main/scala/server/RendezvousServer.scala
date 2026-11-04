package server

import communication.udp.{Hmac, Peer, Rendezvous, UDPSocket}
import communication.udp.Rendezvous.Packet
import menus.data.AllGameCredentials

/** The rendezvous of the UDP hole punching (see `communication.udp.Rendezvous`): remembers where
  * the game servers and the players of the started games are seen from, and tells the game servers
  * where their players are. For the ones seen on a private network, it also remembers the public
  * address they say they have (see `communication.udp.Stun`).
  *
  * Only the registers signed with the secret of their game (the game servers) or of their player
  * are considered, and only if their number is greater than the one of the previous register of the
  * same one (so that a register seen on the way cannot be replayed from elsewhere). The answers are
  * signed with the same secret.
  *
  * @param credentials
  *   the credentials of a started game (it may block)
  */
final class RendezvousServer(
    socket: UDPSocket,
    credentials: String => Option[AllGameCredentials]
) {
  import RendezvousServer.*

  /** `peers`: where it was seen from, and its public address if it gave one; `number`: of its
    * latest register.
    */
  private case class Seen(peers: Vector[Peer], at: Long, number: Long)
  private case class Game(server: Option[Seen], players: Map[String, Seen])

  private val lock                                              = new Object
  private var games                                             = Map.empty[String, Game]
  private var knownCredentials: Map[String, AllGameCredentials] = Map.empty

  def start(port: Int, address: Option[String]): Unit = {
    println(s"Starting rendezvous server on port $port")
    socket.setMessageCallback((datagram, from) =>
      Rendezvous.parse(datagram).foreach(receive(_, from))
    )
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

  private def reply(to: Peer, packet: Packet, key: Hmac): Unit =
    socket.send(Rendezvous.encode(packet, key), to.port, to.address)

  private def receive(received: Rendezvous.Received, from: Peer): Unit = received.packet match {
    case Packet.ServerRegister(gameId, number, publicAddress) =>
      for
        creds <- credentialsOf(gameId)
        key = Hmac(creds.secret)
        if received.signedWith(key)
        registered = Packet.Registered(from, publicAddress.filter(_ => from.onPrivateNetwork))
        game <- seen(
          gameId,
          game =>
            Option.when(game.server.forall(_.number < number))(
              game.copy(server = Some(seenAs(registered, number)))
            )
        )
      do
        val players =
          game.players.toVector.flatMap((name, seen) => seen.peers.map(name -> _))
        reply(from, Packet.Players(registered, players), key)
    case Packet.PlayerRegister(userName, gameId, number, publicAddress) =>
      for
        creds     <- credentialsOf(gameId)
        userCreds <- creds.allGameUserCredentials.find(_.userName == userName)
        key = Hmac(userCreds.userSecret)
        if received.signedWith(key)
        registered = Packet.Registered(from, publicAddress.filter(_ => from.onPrivateNetwork))
        _ <- seen(
          gameId,
          game =>
            Option.when(game.players.get(userName).forall(_.number < number))(
              game.copy(players = game.players + (userName -> seenAs(registered, number)))
            )
        )
      do reply(from, registered, key)
    case _ => () // not for us
  }

  private def seenAs(registered: Packet.Registered, number: Long): Seen =
    Seen((registered.seenAs +: registered.publicAddress.toVector).distinct, now, number)

  private def now: Long = System.currentTimeMillis()

  /** Updates the game (unless `update` gives nothing: then it is not changed, and nothing is
    * returned), and forgets what was not seen for too long.
    */
  private def seen(gameId: String, update: Game => Option[Game]): Option[Game] =
    lock.synchronized {
      val limit   = now - Forget
      val updated = update(games.getOrElse(gameId, Game(None, Map.empty)))
      updated.foreach(game => games += gameId -> game)
      games = games.view
        .mapValues(game =>
          Game(game.server.filter(_.at > limit), game.players.filter(_._2.at > limit))
        )
        .filter((_, game) => game.server.isDefined || game.players.nonEmpty)
        .toMap
      knownCredentials = knownCredentials.filter((gameId, _) => games.contains(gameId))
      updated.flatMap(_ => games.get(gameId))
    }
}

object RendezvousServer {

  /** The game servers say hello every second, the players every half second while connecting. */
  private val Forget = 60000L
}
