package server.udp

import boopickle.Pickler
import communication.udp.*
import communication.udp.Connection.Delivery
import models.bff.ingame.InGameWSProtocol

/** The players connected to the game server through UDP.
  *
  * One socket is shared by everyone. A datagram from an address that is not a known player is only
  * considered if it is part of the handshake (see [[Envelope]]): a hello is challenged, and a proof
  * made with the secret given by `secretOf` creates a [[Connection]] with that address, with the
  * keys of the session. From then on, the datagrams from this address go to that connection (if
  * they are tagged with its key, and not seen before), whose messages end up in the `listener`.
  *
  * A player has at most one session at a time, and an address at most one player: a proof that
  * succeeds for a user that already has a session (e.g., they reconnect from another address)
  * replaces the previous session, as if it was lost.
  *
  * @param secretOf
  *   the secret of this player of this game, otherwise the reason to give (one of
  *   [[Envelope.RejectReason]])
  * @param challenges
  *   the nonces of the challenges (their key must stay the same while the server runs)
  */
final class PlayerSessions(
    socket: UDPSocket,
    timers: Timers,
    secretOf: (String, Int) => Either[Byte, String],
    listener: PlayerSessions.Listener,
    pingDelay: Int = 1000,
    challenges: Envelope.Challenges = Envelope.Challenges(PlayerSessions.randomKey())
)(using Pickler[InGameWSProtocol]) {

  private final class Session(
      val userName: String,
      val peer: Peer,
      val nonces: Envelope.Packet.Challenge,
      val keys: Envelope.SessionKeys,
      val connection: Connection[InGameWSProtocol]
  )

  private val lock                             = new Object
  private var byPeer: Map[Peer, Session]       = Map.empty
  private var byUserName: Map[String, Session] = Map.empty

  def start(port: Int, address: Option[String] = None): Unit = {
    socket.setMessageCallback(receive)
    socket.bind(port, address)
  }

  /** The port the sessions are received on (useful when started on port 0). */
  def localPort: Int = socket.peer.port

  /** Sends a message to a player; does nothing if they have no session (anymore). */
  def send(userName: String, message: InGameWSProtocol, delivery: Delivery): Unit =
    lock.synchronized(byUserName.get(userName)).foreach(_.connection.send(message, delivery))

  def isConnected(userName: String): Boolean = lock.synchronized(byUserName.contains(userName))

  /** Drops all sessions (without telling the listener) and closes the socket. */
  def close(): Unit = {
    val sessions = lock.synchronized {
      val all = byPeer.values.toList
      byPeer = Map.empty
      byUserName = Map.empty
      all
    }
    sessions.foreach(_.connection.disconnect())
    socket.close()
  }

  private def reply(to: Peer, datagram: Array[Byte]): Unit =
    socket.send(datagram, to.port, to.address)

  private def reply(to: Peer, packet: Envelope.Packet): Unit = reply(to, Envelope.encode(packet))

  private def rejected(to: Peer, reason: Byte): Unit = reply(to, Envelope.Packet.Rejected(reason))

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    Envelope.parse(datagram).foreach {
      case hello: Envelope.Packet.Hello =>
        secretOf(hello.userName, hello.gameId) match {
          case Left(reason) => rejected(from, reason)
          case Right(_) => reply(from, challenges.challenge(hello, from))
        }
      case proof: Envelope.Packet.Proof =>
        secretOf(proof.userName, proof.gameId) match {
          case Left(reason) => rejected(from, reason)
          case Right(secret) =>
            val key = Hmac(secret)
            if challenges.isOurs(proof, from) && Envelope.isProven(proof, key) then
              proven(proof.userName, from, proof.nonces, key)
            else rejected(from, Envelope.RejectReason.Unauthorized)
        }
      case data: Envelope.Packet.Data =>
        lock.synchronized(byPeer.get(from)) match {
          case Some(session) => session.keys.open(data).foreach(session.connection.onMessage)
          case None => rejected(from, Envelope.RejectReason.NoSession)
        }
      case _ => () // for clients only
    }

  private def proven(
      userName: String,
      from: Peer,
      nonces: Envelope.Packet.Challenge,
      secret: Hmac
  ): Unit = {
    // (the session, whether it is new, the sessions it replaces)
    val (session, isNew, replaced) = lock.synchronized {
      byPeer.get(from).filter(s => s.userName == userName && s.nonces == nonces) match {
        case Some(existing) => (existing, false, Nil)
        case None =>
          val replaced = (byPeer.get(from).toList ++ byUserName.get(userName)).distinct
          val keys     = Envelope.SessionKeys(secret, nonces, towardsServer = false)
          val connection: Connection[InGameWSProtocol] = new Connection[InGameWSProtocol](
            from,
            new Envelope.DataSocket(socket, keys),
            timers,
            message => listener.message(userName, message),
            onLost = () => lost(userName, from)
          )
          val session = Session(userName, from, nonces, keys, connection)
          byPeer = byPeer -- replaced.map(_.peer) + (from                 -> session)
          byUserName = byUserName -- replaced.map(_.userName) + (userName -> session)
          (session, true, replaced)
      }
    }

    replaced.foreach { session =>
      session.connection.disconnect()
      listener.lost(session.userName)
    }
    // also answered when the session already existed, since the previous answer may have been lost, and
    // before the listener sends anything to the player, so that Accepted comes first (most of the time)
    reply(from, session.keys.accepted)
    if isNew then {
      session.connection.connect(pingDelay)
      listener.connected(userName)
    }
  }

  private def lost(userName: String, peer: Peer): Unit = {
    val wasRegistered = lock.synchronized {
      val registered = byPeer.get(peer).exists(_.userName == userName)
      if registered then {
        byPeer = byPeer - peer
        byUserName = byUserName - userName
      }
      registered
    }
    if wasRegistered then listener.lost(userName)
  }
}

object PlayerSessions {

  /** A key for the challenges, that nobody else knows. */
  def randomKey(): Hmac = {
    val key = new Array[Byte](32)
    java.security.SecureRandom().nextBytes(key)
    Hmac(key)
  }

  /** Callbacks are called from the thread receiving datagrams, or from a timer: they must not
    * block.
    */
  trait Listener {

    /** A player said hello and was accepted. */
    def connected(userName: String): Unit

    def message(userName: String, message: InGameWSProtocol): Unit

    /** The session of this player is over: they went silent, or they connected from elsewhere. */
    def lost(userName: String): Unit
  }
}
