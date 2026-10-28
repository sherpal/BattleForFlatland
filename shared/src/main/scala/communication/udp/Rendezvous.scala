package communication.udp

import boopickle.Default.*
import models.bff.ingame.GameUserCredentials

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
  * This works for the NATs that give the same public address to a socket whatever the destination
  * (most home routers), not for the "symmetric" ones (some corporate and mobile networks).
  */
object Rendezvous {

  sealed trait Packet
  object Packet {
    /** @param publicAddress
      *   our public address, when the menu server sees us on a private network (see [[Stun]])
      */
    case class ServerRegister(gameId: String, gameSecret: String, publicAddress: Option[Peer])
        extends Packet

    /** The players of the game, with the addresses the menu server knows them at (a player may
      * have two, see [[Stun]]), and where it sees the game server from (see [[Registered]]).
      */
    case class Players(you: Registered, players: Vector[(String, Peer)]) extends Packet

    /** @param publicAddress
      *   our public address, when the menu server sees us on a private network (see [[Stun]])
      */
    case class PlayerRegister(credentials: GameUserCredentials, publicAddress: Option[Peer])
        extends Packet

    /** The address the menu server saw the register from, and the public address it took from it
      * (only when the former is on a private network).
      */
    case class Registered(seenAs: Peer, publicAddress: Option[Peer]) extends Packet

    /** Opens the NAT of its sender to its destination: it carries nothing. The game server answers
      * the ones of the players.
      */
    case object Punch extends Packet
  }

  /** Bigger datagrams are not even looked at. */
  val MaxSize = 2048

  import MessageKind.*

  def isRendezvous(datagram: Array[Byte]): Boolean =
    datagram.nonEmpty && datagram(0) >= ServerRegisterKind && datagram(0) <= PunchKind

  private def withKind(kind: MessageKind, bb: ByteBuffer): Array[Byte] = {
    val result = new Array[Byte](bb.remaining() + 1)
    result(0) = kind
    bb.get(result, 1, bb.remaining())
    result
  }

  def encode(packet: Packet): Array[Byte] = packet match {
    case Packet.ServerRegister(gameId, gameSecret, publicAddress) =>
      withKind(ServerRegisterKind, Pickle.intoBytes((gameId, gameSecret, publicAddress)))
    case Packet.Players(Packet.Registered(seenAs, publicAddress), players) =>
      withKind(PlayersKind, Pickle.intoBytes((seenAs, publicAddress, players)))
    case Packet.PlayerRegister(credentials, publicAddress) =>
      withKind(PlayerRegisterKind, Pickle.intoBytes((credentials, publicAddress)))
    case Packet.Registered(seenAs, publicAddress) =>
      withKind(RegisteredKind, Pickle.intoBytes((seenAs, publicAddress)))
    case Packet.Punch => Array(PunchKind)
  }

  /** Decodes a datagram, if it is one of the rendezvous. */
  def parse(datagram: Array[Byte]): Option[Packet] =
    if !isRendezvous(datagram) || datagram.length > MaxSize then None
    else {
      def payload = ByteBuffer.wrap(datagram, 1, datagram.length - 1)
      Try(datagram(0) match {
        case ServerRegisterKind =>
          val (gameId, gameSecret, publicAddress) =
            Unpickle[(String, String, Option[Peer])].fromBytes(payload)
          Packet.ServerRegister(gameId, gameSecret, publicAddress)
        case PlayersKind =>
          val (seenAs, publicAddress, players) =
            Unpickle[(Peer, Option[Peer], Vector[(String, Peer)])].fromBytes(payload)
          Packet.Players(Packet.Registered(seenAs, publicAddress), players)
        case PlayerRegisterKind =>
          val (credentials, publicAddress) =
            Unpickle[(GameUserCredentials, Option[Peer])].fromBytes(payload)
          Packet.PlayerRegister(credentials, publicAddress)
        case RegisteredKind =>
          val (seenAs, publicAddress) = Unpickle[(Peer, Option[Peer])].fromBytes(payload)
          Packet.Registered(seenAs, publicAddress)
        case _ => Packet.Punch
      }).toOption
    }

  /** Makes a [[UDPSocket]] out of another one, whose datagrams of the rendezvous go to
    * `onRendezvous`, and the ones of STUN to `onStun`, instead of the message callback of this
    * socket. Sending and binding are done by the underlying socket (which can be used to send the
    * datagrams of the rendezvous).
    */
  final class SharedSocket(
      underlying: UDPSocket,
      onRendezvous: (Packet, Peer) => Unit,
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
