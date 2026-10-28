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
  *
  * This works for the NATs that give the same public address to a socket whatever the destination
  * (most home routers), not for the "symmetric" ones (some corporate and mobile networks).
  */
object Rendezvous {

  sealed trait Packet
  object Packet {
    case class ServerRegister(gameId: String, gameSecret: String) extends Packet

    /** The players of the game, with the address the menu server saw them from. */
    case class Players(players: Vector[(String, Peer)]) extends Packet

    case class PlayerRegister(credentials: GameUserCredentials) extends Packet

    /** The address the menu server saw the player from. */
    case class Registered(seenAs: Peer) extends Packet

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
    case Packet.ServerRegister(gameId, gameSecret) =>
      withKind(ServerRegisterKind, Pickle.intoBytes((gameId, gameSecret)))
    case Packet.Players(players) => withKind(PlayersKind, Pickle.intoBytes(players))
    case Packet.PlayerRegister(credentials) =>
      withKind(PlayerRegisterKind, Pickle.intoBytes(credentials))
    case Packet.Registered(seenAs) => withKind(RegisteredKind, Pickle.intoBytes(seenAs))
    case Packet.Punch => Array(PunchKind)
  }

  /** Decodes a datagram, if it is one of the rendezvous. */
  def parse(datagram: Array[Byte]): Option[Packet] =
    if !isRendezvous(datagram) || datagram.length > MaxSize then None
    else {
      def payload = ByteBuffer.wrap(datagram, 1, datagram.length - 1)
      Try(datagram(0) match {
        case ServerRegisterKind =>
          val (gameId, gameSecret) = Unpickle[(String, String)].fromBytes(payload)
          Packet.ServerRegister(gameId, gameSecret)
        case PlayersKind => Packet.Players(Unpickle[Vector[(String, Peer)]].fromBytes(payload))
        case PlayerRegisterKind =>
          Packet.PlayerRegister(Unpickle[GameUserCredentials].fromBytes(payload))
        case RegisteredKind => Packet.Registered(Unpickle[Peer].fromBytes(payload))
        case _ => Packet.Punch
      }).toOption
    }

  /** Makes a [[UDPSocket]] out of another one, whose datagrams of the rendezvous go to
    * `onRendezvous` instead of the message callback of this socket. Sending and binding are done by
    * the underlying socket (which can be used to send the datagrams of the rendezvous).
    */
  final class SharedSocket(underlying: UDPSocket, onRendezvous: (Packet, Peer) => Unit)
      extends UDPSocket {
    underlying.setMessageCallback((datagram, from) =>
      if isRendezvous(datagram) then parse(datagram).foreach(onRendezvous(_, from))
      else _onMessage(datagram, from)
    )

    def peer: Peer = underlying.peer

    def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

    def send(message: Array[Byte], port: Int, address: String): Unit =
      underlying.send(message, port, address)

    def close(): Unit = underlying.close()
  }

}
