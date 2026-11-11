package communication.udp

import boopickle.Default.*

import java.nio.ByteBuffer
import scala.util.Try

/** UDP hole punching between the players and the game server, which runs on the computer of one of
  * the players (possibly behind a NAT), through the menu server (the "rendezvous").
  *
  * The datagrams of the rendezvous travel on the same sockets as the [[Envelope]] ones: that is the
  * whole point, since the addresses that the menu server sees them coming from are the ones that
  * the NATs give to the game's sockets. Their first byte is therefore distinct from the
  * [[Envelope]] ones (see [[isRendezvous]]), and [[SharedSocket]] sorts them out.
  *
  *   - The game server says [[Packet.ServerRegister]] to the menu server every second (which also
  *     keeps its NAT mapping alive). The menu server answers [[Packet.Players]]: the addresses it
  *     saw the players of the game from so far. The game server sends [[Packet.Punch]] to the ones
  *     it has no session with yet, which opens its NAT to them.
  *   - The players say [[Packet.PlayerRegister]] to the menu server, from the socket they play
  *     with, until it answers [[Packet.Registered]]. Meanwhile, they send [[Packet.Punch]] to every
  *     address of the game server they know of (the one the menu server sees it from, its addresses
  *     on its local network, and the loopback for the player that runs it), which opens their NAT
  *     to the game server. The game server answers them (with a [[Packet.Punch]] too): the first
  *     address the player receives one from is the one they say hello to (see [[Envelope]]), and
  *     play with. (Saying hello to several addresses would make the game server believe that the
  *     player keeps reconnecting from another one.)
  *   - The menu server tells both where it sees them from. When it is on a private network (the
  *     menu server runs on their computer, or on their local network), it is not their public
  *     address: they ask a STUN server for it (see [[Stun]]), and say it in their next registers.
  *     The menu server keeps both addresses (the private one works for the players of that
  *     network), and says which public address it took, so that they know when it is done.
  *
  * All of them but the punches (which carry nothing) are signed ([[Signed]]): the ones about the
  * game server with the secret of the game, the ones about a player with theirs. The secrets never
  * travel here (they come from the menu server, in HTTPS). The registers are numbered, so that they
  * cannot be replayed (from another address, to divert the punches there).
  *
  * This works for the NATs that give the same public address to a socket whatever the destination
  * (most home routers), not for the "symmetric" ones (some corporate and mobile networks).
  */
object Rendezvous {

  sealed trait Packet
  object Packet {

    /** @param number
      *   greater than the one of the previous register (see [[Counter]])
      * @param publicAddress
      *   our public address, when the menu server sees us on a private network (see [[Stun]])
      */
    case class ServerRegister(gameId: Int, number: Long, publicAddress: Option[Peer]) extends Packet

    /** The players of the game, with the addresses the menu server knows them at (a player may have
      * two, see [[Stun]]), and where it sees the game server from (see [[Registered]]).
      */
    case class Players(you: Registered, players: Vector[(String, Peer)]) extends Packet

    /** @param number
      *   greater than the one of the previous register (see [[Counter]])
      * @param publicAddress
      *   our public address, when the menu server sees us on a private network (see [[Stun]])
      */
    case class PlayerRegister(
        userName: String,
        gameId: Int,
        number: Long,
        publicAddress: Option[Peer]
    ) extends Packet

    /** The address the menu server saw the register from, and the public address it took from it
      * (only when the former is on a private network).
      */
    case class Registered(seenAs: Peer, publicAddress: Option[Peer]) extends Packet

    /** Opens the NAT of its sender to its destination: it carries nothing. The game server answers
      * the ones of the players.
      */
    case object Punch extends Packet
  }

  /** A packet received, and whether it was signed with a key (always false for the punches). */
  final case class Received(packet: Packet, signedWith: Hmac => Boolean)

  /** Bigger datagrams are not even looked at. */
  val MaxSize = 2048

  /** The numbers of the registers: the time (in milliseconds), or one more than the previous one,
    * so that they keep increasing, also when the program runs again (unless the clock went back).
    */
  final class Counter {
    private var last = 0L

    def next(): Long = synchronized {
      last = math.max(last + 1, System.currentTimeMillis())
      last
    }
  }

  import MessageKind.*

  def isRendezvous(datagram: Array[Byte]): Boolean =
    datagram.nonEmpty && datagram(0) >= ServerRegisterKind && datagram(0) <= PunchKind

  /** The datagram of the packet, signed with `key` (the punches are not signed: no key needed). */
  def encode(packet: Packet, key: Hmac): Array[Byte] = packet match {
    case Packet.ServerRegister(gameId, number, publicAddress) =>
      Signed.seal(ServerRegisterKind, Pickle.intoBytes((gameId, number, publicAddress)), key)
    case Packet.Players(Packet.Registered(seenAs, publicAddress), players) =>
      Signed.seal(PlayersKind, Pickle.intoBytes((seenAs, publicAddress, players)), key)
    case Packet.PlayerRegister(userName, gameId, number, publicAddress) =>
      Signed.seal(
        PlayerRegisterKind,
        Pickle.intoBytes((userName, gameId, number, publicAddress)),
        key
      )
    case Packet.Registered(seenAs, publicAddress) =>
      Signed.seal(RegisteredKind, Pickle.intoBytes((seenAs, publicAddress)), key)
    case Packet.Punch => punch
  }

  val punch: Array[Byte] = Array(PunchKind)

  /** Decodes a datagram, if it is one of the rendezvous. Whether it is signed by who it claims to
    * come from is for the receiver to check, with the key of that one. Only for the JVM, where
    * decoding garbage fails cleanly: elsewhere, see [[parseSignedWith]].
    */
  def parse(datagram: Array[Byte]): Option[Received] =
    if !isRendezvous(datagram) || datagram.length > MaxSize then None
    else if datagram(0) == PunchKind then Some(Received(Packet.Punch, _ => false))
    else
      Signed.open(datagram).flatMap(signed => decode(signed).map(Received(_, signed.isSignedWith)))

  /** Decodes a datagram of the rendezvous, if it is signed with `key` (checked first: only what the
    * other side made is decoded), or a punch.
    */
  def parseSignedWith(datagram: Array[Byte], key: Hmac): Option[Packet] =
    if !isRendezvous(datagram) || datagram.length > MaxSize then None
    else if datagram(0) == PunchKind then Some(Packet.Punch)
    else Signed.open(datagram).filter(_.isSignedWith(key)).flatMap(decode)

  private def decode(signed: Signed.Opened): Option[Packet] =
    Try(signed.kind match {
      case ServerRegisterKind =>
        val (gameId, number, publicAddress) =
          Unpickle[(Int, Long, Option[Peer])].fromBytes(signed.payload)
        Packet.ServerRegister(gameId, number, publicAddress)
      case PlayersKind =>
        val (seenAs, publicAddress, players) =
          Unpickle[(Peer, Option[Peer], Vector[(String, Peer)])].fromBytes(signed.payload)
        Packet.Players(Packet.Registered(seenAs, publicAddress), players)
      case PlayerRegisterKind =>
        val (userName, gameId, number, publicAddress) =
          Unpickle[(String, Int, Long, Option[Peer])].fromBytes(signed.payload)
        Packet.PlayerRegister(userName, gameId, number, publicAddress)
      case RegisteredKind =>
        val (seenAs, publicAddress) = Unpickle[(Peer, Option[Peer])].fromBytes(signed.payload)
        Packet.Registered(seenAs, publicAddress)
    }).toOption

  /** Makes a [[UDPSocket]] out of another one, whose datagrams of the rendezvous go to
    * `onRendezvous`, and the ones of STUN to `onStun`, instead of the message callback of this
    * socket. Sending and binding are done by the underlying socket (which can be used to send the
    * datagrams of the rendezvous).
    */
  final class SharedSocket(
      underlying: UDPSocket,
      onRendezvous: (Received, Peer) => Unit,
      onStun: Array[Byte] => Unit
  ) extends UDPSocket {
    underlying.setMessageCallback((datagram, from) =>
      if isRendezvous(datagram) then parse(datagram).foreach(onRendezvous(_, from))
      else if Stun.isStun(datagram) then onStun(datagram)
      else _onMessage(datagram, from)
    )

    def peer: Peer = underlying.peer

    def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

    def send(message: Array[Byte], port: Int, address: String): Unit =
      underlying.send(message, port, address)

    def close(): Unit = underlying.close()
  }

}

/** Datagrams made of their kind, their payload, and the tag of both (see [[Hmac]]). */
object Signed {
  final case class Opened(kind: Byte, payload: ByteBuffer, signed: Array[Byte], tag: Array[Byte]) {
    def isSignedWith(key: Hmac): Boolean = key.verify(tag, signed)
  }

  def seal(kind: MessageKind, payload: ByteBuffer, key: Hmac): Array[Byte] = {
    val signed = new Array[Byte](payload.remaining() + 1)
    signed(0) = kind
    payload.get(signed, 1, payload.remaining())
    Hmac.concat(Seq(signed, key.tag(signed)))
  }

  def open(datagram: Array[Byte]): Option[Opened] =
    Option.when(datagram.length > Hmac.TagSize) {
      val signed = datagram.dropRight(Hmac.TagSize)
      Opened(
        datagram(0),
        ByteBuffer.wrap(signed, 1, signed.length - 1),
        signed,
        datagram.takeRight(Hmac.TagSize)
      )
    }
}
