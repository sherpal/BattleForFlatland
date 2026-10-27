package server.udp

import boopickle.Pickler
import communication.udp.*
import communication.udp.Connection.Delivery
import models.bff.ingame.{GameUserCredentials, InGameWSProtocol}

/** The players connected to the game server through UDP.
  *
  * One socket is shared by everyone. A datagram from an address that is not a known player is only
  * considered if it says hello (see [[Envelope]]) with credentials accepted by `authenticate`. This
  * creates a [[Connection]] with that address, and from then on the datagrams from this address go
  * to that connection, whose messages end up in the `listener`.
  *
  * A player has at most one session at a time, and an address at most one player: a hello that
  * accepted for a user that already has a session (e.g., they reconnect from another address)
  * replaces the previous session, as if it was lost.
  *
  * Nothing here is authenticated beyond the hello: the address of a datagram can be spoofed.
  *
  * @param authenticate
  *   `Right` if these credentials are the ones of a player of this game, otherwise the reason to
  *   give (one of [[Envelope.RejectReason]])
  */
final class PlayerSessions(
    socket: UDPSocket,
    timers: Timers,
    authenticate: GameUserCredentials => Either[Byte, Unit],
    listener: PlayerSessions.Listener,
    pingDelay: Int = 1000
)(using Pickler[InGameWSProtocol]) {

  private final class Session(
      val userName: String,
      val peer: Peer,
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

  private def receive(datagram: Array[Byte], from: Peer): Unit =
    Envelope.parse(datagram).foreach {
      case Envelope.Packet.Hello(credentials) =>
        authenticate(credentials) match {
          case Left(reason) => reply(from, Envelope.rejected(reason))
          case Right(()) => hello(credentials.userName, from)
        }
      case Envelope.Packet.Data(payload) =>
        lock.synchronized(byPeer.get(from)) match {
          case Some(session) => session.connection.onMessage(payload)
          case None => reply(from, Envelope.rejected(Envelope.RejectReason.NoSession))
        }
      case Envelope.Packet.Accepted | Envelope.Packet.Rejected(_) => () // for clients only
    }

  private def hello(userName: String, from: Peer): Unit = {
    // (the session if it is new, the sessions it replaces)
    val (newSession, replaced) = lock.synchronized {
      if byPeer.get(from).exists(_.userName == userName) then (None, Nil)
      else {
        val replaced = (byPeer.get(from).toList ++ byUserName.get(userName)).distinct
        val connection: Connection[InGameWSProtocol] = new Connection[InGameWSProtocol](
          from,
          new Envelope.DataSocket(socket),
          timers,
          message => listener.message(userName, message),
          onLost = () => lost(userName, from)
        )
        val session = Session(userName, from, connection)
        byPeer = byPeer -- replaced.map(_.peer) + (from                 -> session)
        byUserName = byUserName -- replaced.map(_.userName) + (userName -> session)
        (Some(session), replaced)
      }
    }

    replaced.foreach { session =>
      session.connection.disconnect()
      listener.lost(session.userName)
    }
    // also answered when the session already existed, since the previous answer may have been lost, and
    // before the listener sends anything to the player, so that Accepted comes first (most of the time)
    reply(from, Envelope.accepted)
    newSession.foreach { session =>
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
