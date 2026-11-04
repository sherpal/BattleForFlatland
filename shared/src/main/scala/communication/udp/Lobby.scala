package communication.udp

import boopickle.Default.*

import scala.util.Try

/** How the menu server tells the clients that something happened in the menus (see
  * `menus.data.MenuGameComm`), without them asking all the time, nor keeping a connection open: in
  * UDP, on the port of the rendezvous.
  *
  *   - The client subscribes in HTTPS (`api/bff/lobby/subscribe`), which gives it the id of its
  *     subscription, and a key of its own (`menus.data.LobbySubscription`).
  *   - It says [[Packet.Hello]] to the menu server every few seconds, from the same socket: so the
  *     menu server knows where it is (and that it is still there), and the NAT of the client lets
  *     the answers in.
  *   - The menu server answers [[Packet.Poke]], with the number of the latest message for this
  *     subscription. It also pokes the client as soon as there is a new one.
  *   - The client gets the messages it did not see yet in HTTPS (`api/bff/lobby/messages`), once
  *     per [[Lobby.PokeWindow]] at most, however many pokes came.
  *
  * The messages themselves (which may hold secrets) only travel in HTTPS. The pokes and hellos are
  * signed with the key of the subscription (see [[Signed]]), and the hellos are numbered, so that
  * nobody else can make the menu server believe that the client is still there, nor send its pokes
  * elsewhere.
  */
object Lobby {

  sealed trait Packet
  object Packet {

    /** @param number
      *   greater than the one of the previous hello (see [[Rendezvous.Counter]])
      */
    case class Hello(subscription: String, number: Long) extends Packet

    /** @param latest
      *   the number of the latest message of the subscription (0 when there is none)
      */
    case class Poke(subscription: String, latest: Long) extends Packet
  }

  /** The client says hello this often (most NATs forget a mapping after 30 seconds or more). */
  val HelloInterval = 5000

  /** The menu server deems the client gone when it did not say hello for this long. */
  val Timeout = 20000

  /** The pokes received within this time make one request for the messages. */
  val PokeWindow = 500

  val MaxSize = 512

  import MessageKind.*

  def isLobby(datagram: Array[Byte]): Boolean =
    datagram.nonEmpty && (datagram(0) == LobbyHelloKind || datagram(0) == LobbyPokeKind)

  def encode(packet: Packet, key: Hmac): Array[Byte] = packet match {
    case Packet.Hello(subscription, number) =>
      Signed.seal(LobbyHelloKind, Pickle.intoBytes((subscription, number)), key)
    case Packet.Poke(subscription, latest) =>
      Signed.seal(LobbyPokeKind, Pickle.intoBytes((subscription, latest)), key)
  }

  /** A packet received, and whether it was signed with a key. */
  final case class Received(packet: Packet, signedWith: Hmac => Boolean)

  /** Decodes a datagram, if it is one of the lobby. Whether it is signed with the key of its
    * subscription is for the receiver to check. Only for the JVM, where decoding garbage fails
    * cleanly: elsewhere, see [[parseSignedWith]].
    */
  def parse(datagram: Array[Byte]): Option[Received] =
    if !isLobby(datagram) || datagram.length > MaxSize then None
    else
      Signed.open(datagram).flatMap(signed => decode(signed).map(Received(_, signed.isSignedWith)))

  /** Decodes a datagram of the lobby, if it is signed with `key` (checked first: only what the
    * other side made is decoded).
    */
  def parseSignedWith(datagram: Array[Byte], key: Hmac): Option[Packet] =
    if !isLobby(datagram) || datagram.length > MaxSize then None
    else Signed.open(datagram).filter(_.isSignedWith(key)).flatMap(decode)

  private def decode(signed: Signed.Opened): Option[Packet] =
    Try {
      val (subscription, number) = Unpickle[(String, Long)].fromBytes(signed.payload)
      if signed.kind == LobbyHelloKind then Packet.Hello(subscription, number)
      else Packet.Poke(subscription, number)
    }.toOption

  /** Makes a [[UDPSocket]] out of another one, whose datagrams of the lobby go to `onLobby` instead
    * of the message callback of this socket (the menu server receives both the ones of the lobby
    * and of the rendezvous on its UDP port).
    */
  final class SharedSocket(underlying: UDPSocket, onLobby: (Received, Peer) => Unit)
      extends UDPSocket {
    underlying.setMessageCallback((datagram, from) =>
      if isLobby(datagram) then parse(datagram).foreach(onLobby(_, from))
      else _onMessage(datagram, from)
    )

    def peer: Peer = underlying.peer

    def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

    def send(message: Array[Byte], port: Int, address: String): Unit =
      underlying.send(message, port, address)

    def close(): Unit = underlying.close()
  }
}
