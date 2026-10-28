package communication.udp

import scala.annotation.tailrec
import scala.util.Random
import scala.util.control.NonFatal

/** Finds out the public address of a UDP socket behind a NAT, by asking a STUN server (RFC 8489,
  * only its binding request) from that socket: it answers with the address it saw the request come
  * from.
  *
  * The rendezvous does that for the game's sockets (see [[Rendezvous]]), except for the ones it
  * sees on a private network: when the menu server runs on the computer of a player, or on their
  * local network, their datagrams do not go through their NAT to reach it. They ask a STUN server
  * instead (see [[Lookup]]), and tell the rendezvous what it answered.
  *
  * The datagrams of STUN travel on the game's sockets, next to the [[Envelope]] and [[Rendezvous]]
  * ones: [[isStun]] tells them apart.
  */
object Stun {

  /** A public STUN server (Google's), which answers anyone. */
  val DefaultServer: Peer = Peer("stun.l.google.com", 19302)

  private val MagicCookie      = 0x2112a442
  private val HeaderSize       = 20
  private val BindingRequest   = 0x0001
  private val BindingSuccess   = 0x0101
  private val MappedAddress    = 0x0001
  private val XorMappedAddress = 0x0020
  private val Ipv4Family       = 0x01

  def newTransactionId(): Array[Byte] = {
    val id = new Array[Byte](12)
    Random.nextBytes(id)
    id
  }

  def request(transactionId: Array[Byte]): Array[Byte] = {
    val datagram = new Array[Byte](HeaderSize)
    writeShort(datagram, 0, BindingRequest)
    writeInt(datagram, 4, MagicCookie)
    System.arraycopy(transactionId, 0, datagram, 8, 12)
    datagram
  }

  /** Whether the datagram is one of STUN: it has the magic cookie, and the length it says is its
    * own. (Our other datagrams could start the same, but never have both.)
    */
  def isStun(datagram: Array[Byte]): Boolean =
    datagram.length >= HeaderSize && readInt(datagram, 4) == MagicCookie &&
      readShort(datagram, 2) == datagram.length - HeaderSize

  /** The address in the answer to our request with `transactionId`, if it is one (IPv4 only). */
  def parse(datagram: Array[Byte], transactionId: Array[Byte]): Option[Peer] =
    if !isStun(datagram) || readShort(datagram, 0) != BindingSuccess ||
      !datagram.slice(8, HeaderSize).sameElements(transactionId)
    then None
    else {
      @tailrec def addresses(offset: Int, found: Map[Int, Peer]): Map[Int, Peer] =
        if offset + 4 > datagram.length then found
        else {
          val kind   = readShort(datagram, offset)
          val length = readShort(datagram, offset + 2)
          val value  = offset + 4
          if value + length > datagram.length then found
          else
            addresses(
              value + (length + 3) / 4 * 4, // attributes are aligned on 4 bytes
              address(datagram, kind, value, length).fold(found)(found.updated(kind, _))
            )
        }
      val found = addresses(HeaderSize, Map.empty)
      found.get(XorMappedAddress).orElse(found.get(MappedAddress))
    }

  private def address(datagram: Array[Byte], kind: Int, at: Int, length: Int): Option[Peer] =
    Option.when(
      (kind == XorMappedAddress || kind == MappedAddress) && length >= 8 &&
        datagram(at + 1) == Ipv4Family
    ) {
      val mask = if kind == XorMappedAddress then MagicCookie else 0
      val port = readShort(datagram, at + 2) ^ (mask >>> 16)
      val ip   = readInt(datagram, at + 4) ^ mask
      Peer(Seq(24, 16, 8, 0).map(shift => (ip >>> shift) & 0xff).mkString("."), port)
    }

  private def readShort(datagram: Array[Byte], at: Int): Int =
    ((datagram(at) & 0xff) << 8) | (datagram(at + 1) & 0xff)

  private def readInt(datagram: Array[Byte], at: Int): Int =
    (readShort(datagram, at) << 16) | readShort(datagram, at + 2)

  private def writeShort(datagram: Array[Byte], at: Int, value: Int): Unit = {
    datagram(at) = (value >>> 8).toByte
    datagram(at + 1) = value.toByte
  }

  private def writeInt(datagram: Array[Byte], at: Int, value: Int): Unit = {
    writeShort(datagram, at, value >>> 16)
    writeShort(datagram, at + 2, value)
  }

  /** Looks up the public address of `socket`, when the rendezvous sees it on a private network (see
    * [[answered]]): asks `server` every half second (a datagram may be lost), until it answers, or
    * for 3 seconds. The STUN datagrams that the socket receives must be given to [[received]].
    *
    * @param onFound
    *   called with our public address, when the server answers
    */
  final class Lookup(
      socket: UDPSocket,
      timers: Timers,
      onFound: Peer => Unit = _ => (),
      server: Peer = DefaultServer
  ) {
    private val transactionId = newTransactionId()
    private val requestBytes  = request(transactionId)

    @volatile private var started  = false
    @volatile private var over     = false
    @volatile private var _address = Option.empty[Peer]
    @volatile private var retrying = Option.empty[Timers.TimerHandler]

    /** Our public address, once the server answered. */
    def address: Option[Peer] = _address

    /** Tells how the rendezvous answered our register: the address it saw it from, and the public
      * address it took from it. When it sees us on a private network, it starts the lookup (once).
      *
      * @return
      *   whether the rendezvous knows all it can about us (the lookup is over, and it took our
      *   public address, if we found one)
      */
    def answered(seenAs: Peer, publicAddress: Option[Peer]): Boolean =
      !seenAs.onPrivateNetwork || {
        start()
        over && publicAddress == _address
      }

    private def start(): Unit = if !started then {
      started = true
      println(s"The rendezvous sees us on a private network: asking $server our public address")
      send()
      retrying = Some(timers.setInterval(RetryInterval)(send()))
      timers.setTimeout(Timeout)(if !over then {
        finish()
        println(s"$server did not tell us our public address")
      })
    }

    private def send(): Unit =
      try socket.send(requestBytes, server.port, server.address)
      catch { case NonFatal(e) => println(s"Cannot ask $server our public address: $e") }

    private def finish(): Unit = {
      over = true
      retrying.foreach(_.cancel())
    }

    def received(datagram: Array[Byte]): Unit =
      if !over then
        parse(datagram, transactionId).foreach { address =>
          _address = Some(address)
          finish()
          println(s"Our public address, according to $server: $address")
          onFound(address)
        }
  }

  private val RetryInterval = 500
  private val Timeout       = 3000
}
