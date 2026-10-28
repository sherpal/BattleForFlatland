package communication.udp

import boopickle.Default.*
import models.bff.ingame.GameUserCredentials

import java.nio.ByteBuffer
import scala.util.Try

/** What travels in a datagram between a player and the game server, around the packets of a
  * [[Connection]]: the first byte tells what the datagram is.
  *
  * A [[Connection]] knows nothing about who is on the other side. The server therefore only creates
  * one once a datagram from a new address has proven that it comes from a legit player: the player
  * sends [[Packet.Hello]] with their credentials until they receive [[Packet.Accepted]] (or
  * [[Packet.Rejected]]), and only then do they exchange [[Packet.Data]], which contain the packets
  * of the [[Connection]].
  *
  * Hello and Data both being possibly lost, the client must keep sending Hello until accepted; the
  * server answers every one of them, so Hello is idempotent. A server receiving Data from an
  * address it does not know (e.g., after a restart, or once the connection was dropped) answers
  * `Rejected(RejectReason.NoSession)`.
  */
object Envelope {

  sealed trait Packet
  object Packet {
    case class Hello(credentials: GameUserCredentials) extends Packet
    case object Accepted extends Packet
    case class Rejected(reason: Byte) extends Packet
    case class Data(payload: Array[Byte]) extends Packet
  }

  object RejectReason {

    /** The server is not ready to accept players yet. */
    val NotReady: Byte = 0

    /** Wrong game, unknown player or wrong secret. */
    val Unauthorized: Byte = 1

    /** Data was received from an address that did not say hello. */
    val NoSession: Byte = 2
  }

  /** Hello is much smaller than this: bigger datagrams are not even looked at. */
  val MaxHelloSize = 512

  import MessageKind.*

  def hello(credentials: GameUserCredentials): Array[Byte] = {
    val bb      = Pickle.intoBytes(credentials)
    val payload = new Array[Byte](bb.remaining())
    bb.get(payload)
    HelloKind +: payload
  }

  val accepted: Array[Byte] = Array(AcceptedKind)

  def rejected(reason: Byte): Array[Byte] = Array(RejectedKind, reason)

  def data(payload: Array[Byte]): Array[Byte] = {
    val result = new Array[Byte](payload.length + 1)
    result(0) = DataKind
    System.arraycopy(payload, 0, result, 1, payload.length)
    result
  }

  /** Decodes a datagram, if it is one. */
  def parse(datagram: Array[Byte]): Option[Packet] =
    if datagram.isEmpty then None
    else
      datagram(0) match {
        case DataKind => Some(Packet.Data(datagram.drop(1)))
        case AcceptedKind => Some(Packet.Accepted)
        case RejectedKind => Option.when(datagram.length == 2)(Packet.Rejected(datagram(1)))
        case HelloKind if datagram.length <= MaxHelloSize =>
          Try(
            Unpickle[GameUserCredentials].fromBytes(
              ByteBuffer.wrap(datagram, 1, datagram.length - 1)
            )
          ).toOption
            .map(Packet.Hello(_))
        case _ => None
      }

  /** Makes a [[UDPSocket]] out of another one that wraps everything it sends in a [[Packet.Data]].
    * Used by both sides to give to a [[Connection]], after the handshake. The other methods are the
    * ones of the underlying socket.
    */
  final class DataSocket(underlying: UDPSocket) extends UDPSocket {
    def peer: Peer = underlying.peer

    def bind(port: Int, address: Option[String]): Unit = underlying.bind(port, address)

    def send(message: Array[Byte], port: Int, address: String): Unit =
      underlying.send(data(message), port, address)

    def close(): Unit = underlying.close()
  }

}
